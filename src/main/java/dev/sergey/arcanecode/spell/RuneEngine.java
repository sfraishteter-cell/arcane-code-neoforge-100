package dev.sergey.arcanecode.spell;

import dev.sergey.arcanecode.item.RuneStaffItem;
import dev.sergey.arcanecode.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Server-authoritative interpreter for geometric rune programs. */
public final class RuneEngine {
    private static final int OPERATION_LIMIT = 8_192;
    private static final int RECURSION_LIMIT = 48;
    private static final int LOOP_LIMIT = 256;
    private static final double WORLD_EDIT_RANGE = 48.0;

    private RuneEngine() {}

    public static ItemStack findStaff(ServerPlayer player) {
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (stack.getItem() instanceof RuneStaffItem) return stack;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof RuneStaffItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static Result cast(ServerPlayer player, ItemStack staff, List<String> program) {
        if (program.isEmpty()) return Result.fail("Пустая программа");
        Context context = new Context(player, staff);
        try {
            executeProgram(context, program, 0);
            String top = context.stack.peek() == null ? "пусто" : context.stack.peek().shortDisplay();
            player.displayClientMessage(Component.literal("§aПлетение завершено · операций: " + context.operations + " · вершина: " + top), true);
            return Result.ok("Плетение завершено");
        } catch (HaltSignal ignored) {
            return Result.ok("Плетение остановлено");
        } catch (Mishap mishap) {
            showMishap(player, mishap);
            return Result.fail(mishap.getMessage());
        } catch (BreakSignal | ContinueSignal signal) {
            Mishap mishap = new Mishap(MishapKind.CONTROL, "Управление циклом использовано вне цикла");
            showMishap(player, mishap);
            return Result.fail(mishap.getMessage());
        }
    }

    private static void executeProgram(Context context, List<String> program, int depth) {
        if (depth > RECURSION_LIMIT) throw new Mishap(MishapKind.LIMIT, "Превышена глубина вложенных программ");
        Deque<String> queue = new ArrayDeque<>(program);
        while (!queue.isEmpty()) {
            if (++context.operations > OPERATION_LIMIT) throw new Mishap(MishapKind.LIMIT, "Превышен предел операций");
            String id = queue.removeFirst();
            RuneDefinition rune = RuneLibrary.byId(id);
            if (rune == null) throw new Mishap(MishapKind.UNKNOWN, "Неизвестная руна: " + id);
            if (!MediaManager.consume(context.player, context.staff, rune.mediaCost())) {
                throw new Mishap(MishapKind.MEDIA, "Недостаточно Арканной пыли");
            }

            if ("quote_next".equals(id)) {
                if (queue.isEmpty()) throw new Mishap(MishapKind.CONTROL, "После цитирования нет руны");
                context.stack.push(Iota.program(List.of(queue.removeFirst())));
                continue;
            }
            if ("quote_n".equals(id)) {
                int count = (int)bounded(number(pop(context.stack)), 0, 256);
                if (queue.size() < count) throw new Mishap(MishapKind.CONTROL, "Для цитаты не хватает рун");
                List<String> quoted = new ArrayList<>(count);
                for (int i = 0; i < count; i++) quoted.add(queue.removeFirst());
                context.stack.push(Iota.program(quoted));
                continue;
            }
            if ("halt".equals(id)) throw new HaltSignal();
            execute(context, queue, rune, depth);
        }
    }

    private static void execute(Context ctx, Deque<String> queue, RuneDefinition rune, int depth) {
        String id = rune.id();
        if (id.startsWith("effect/")) { ctx.stack.push(Iota.effect(ResourceLocation.parse(rune.argument()))); return; }
        if (id.startsWith("particle/")) { ctx.stack.push(Iota.particle(ResourceLocation.parse(rune.argument()))); return; }
        if (id.startsWith("block/")) { ctx.stack.push(Iota.block(ResourceLocation.parse(rune.argument()))); return; }
        if (id.startsWith("item/")) { ctx.stack.push(Iota.item(ResourceLocation.parse(rune.argument()))); return; }
        if (id.startsWith("sound/")) { ctx.stack.push(Iota.sound(ResourceLocation.parse(rune.argument()))); return; }
        if (id.startsWith("entity_type/")) { ctx.stack.push(Iota.entityType(ResourceLocation.parse(rune.argument()))); return; }

        Deque<Iota> stack = ctx.stack;
        ServerPlayer player = ctx.player;
        switch (id) {
            case "zero" -> stack.push(Iota.number(0)); case "one" -> stack.push(Iota.number(1));
            case "two" -> stack.push(Iota.number(2)); case "three" -> stack.push(Iota.number(3));
            case "four" -> stack.push(Iota.number(4)); case "five" -> stack.push(Iota.number(5));
            case "six" -> stack.push(Iota.number(6)); case "seven" -> stack.push(Iota.number(7));
            case "eight" -> stack.push(Iota.number(8)); case "nine" -> stack.push(Iota.number(9));
            case "ten" -> stack.push(Iota.number(10)); case "hundred" -> stack.push(Iota.number(100));
            case "pi" -> stack.push(Iota.number(Math.PI)); case "e" -> stack.push(Iota.number(Math.E));
            case "true" -> stack.push(Iota.bool(true)); case "false" -> stack.push(Iota.bool(false));
            case "null" -> stack.push(Iota.nil());
            case "self" -> stack.push(Iota.entity(player));
            case "self_pos" -> stack.push(Iota.vector(player.position()));
            case "eye_pos" -> stack.push(Iota.vector(player.getEyePosition()));
            case "look_vec" -> stack.push(Iota.vector(player.getLookAngle()));
            case "up" -> stack.push(Iota.vector(new Vec3(0, 1, 0)));
            case "down" -> stack.push(Iota.vector(new Vec3(0, -1, 0)));
            case "world_time" -> stack.push(Iota.number(player.serverLevel().getDayTime()));
            case "is_raining" -> stack.push(Iota.bool(player.serverLevel().isRaining()));

            case "dup" -> stack.push(require(stack, "Пустой стек"));
            case "swap" -> { Iota a = pop(stack); Iota b = pop(stack); stack.push(a); stack.push(b); }
            case "drop" -> pop(stack);
            case "over" -> { Iota a = pop(stack); Iota b = pop(stack); stack.push(b); stack.push(a); stack.push(b); }
            case "rot" -> { Iota a = pop(stack); Iota b = pop(stack); Iota c = pop(stack); stack.push(b); stack.push(a); stack.push(c); }
            case "clear" -> stack.clear();
            case "depth" -> stack.push(Iota.number(stack.size()));
            case "pack2" -> { Iota a = pop(stack); Iota b = pop(stack); stack.push(Iota.list(List.of(b, a))); }
            case "pack3" -> { Iota a = pop(stack); Iota b = pop(stack); Iota c = pop(stack); stack.push(Iota.list(List.of(c, b, a))); }
            case "unpack" -> { List<Iota> values = list(pop(stack)); for (Iota value : values) stack.push(value); }
            case "list_size" -> stack.push(Iota.number(list(pop(stack)).size()));
            case "list_get" -> { int index = (int)number(pop(stack)); List<Iota> values = list(pop(stack)); if (index < 0 || index >= values.size()) throw new Mishap(MishapKind.VALUE, "Индекс вне списка"); stack.push(values.get(index)); }
            case "list_append" -> { Iota value = pop(stack); List<Iota> values = new ArrayList<>(list(pop(stack))); values.add(value); stack.push(Iota.list(values)); }
            case "list_concat" -> { List<Iota> b = list(pop(stack)); List<Iota> a = new ArrayList<>(list(pop(stack))); a.addAll(b); stack.push(Iota.list(a)); }
            case "list_reverse" -> { List<Iota> values = new ArrayList<>(list(pop(stack))); java.util.Collections.reverse(values); stack.push(Iota.list(values)); }

            case "add" -> binaryNumber(stack, (a,b) -> a+b); case "sub" -> binaryNumber(stack, (a,b) -> a-b);
            case "mul" -> binaryNumber(stack, (a,b) -> a*b); case "div" -> binaryNumber(stack, (a,b) -> { if (b==0) throw new Mishap(MishapKind.VALUE, "Деление на ноль"); return a/b; });
            case "mod" -> binaryNumber(stack, (a,b) -> { if (b==0) throw new Mishap(MishapKind.VALUE, "Деление на ноль"); return a%b; });
            case "pow" -> binaryNumber(stack, Math::pow); case "min" -> binaryNumber(stack, Math::min); case "max" -> binaryNumber(stack, Math::max);
            case "atan2" -> binaryNumber(stack, Math::atan2);
            case "sqrt" -> unaryNumber(stack, a -> { if (a < 0) throw new Mishap(MishapKind.VALUE, "Корень из отрицательного числа"); return Math.sqrt(a); });
            case "abs" -> unaryNumber(stack, Math::abs); case "neg" -> unaryNumber(stack, a -> -a);
            case "floor" -> unaryNumber(stack, Math::floor); case "ceil" -> unaryNumber(stack, Math::ceil); case "round" -> unaryNumber(stack, a -> (double)Math.round(a));
            case "sin" -> unaryNumber(stack, Math::sin); case "cos" -> unaryNumber(stack, Math::cos); case "tan" -> unaryNumber(stack, Math::tan);
            case "log" -> unaryNumber(stack, a -> { if (a <= 0) throw new Mishap(MishapKind.VALUE, "Логарифм требует положительное число"); return Math.log(a); });
            case "exp" -> unaryNumber(stack, Math::exp);
            case "clamp" -> { double max = number(pop(stack)); double min = number(pop(stack)); double value = number(pop(stack)); stack.push(Iota.number(Math.max(min, Math.min(max, value)))); }
            case "lerp" -> { double t = number(pop(stack)); double b = number(pop(stack)); double a = number(pop(stack)); stack.push(Iota.number(a + (b-a)*t)); }
            case "random" -> { double max = number(pop(stack)); double min = number(pop(stack)); if (max < min) { double tmp=min; min=max; max=tmp; } stack.push(Iota.number(max == min ? min : ThreadLocalRandom.current().nextDouble(min, max))); }

            case "eq" -> { Iota b = pop(stack); Iota a = pop(stack); stack.push(Iota.bool(a.equals(b))); }
            case "neq" -> { Iota b = pop(stack); Iota a = pop(stack); stack.push(Iota.bool(!a.equals(b))); }
            case "lt" -> compare(stack, (a,b) -> a < b); case "lte" -> compare(stack, (a,b) -> a <= b);
            case "gt" -> compare(stack, (a,b) -> a > b); case "gte" -> compare(stack, (a,b) -> a >= b);
            case "not" -> stack.push(Iota.bool(!bool(pop(stack))));
            case "and" -> binaryBool(stack, (a,b) -> a && b); case "or" -> binaryBool(stack, (a,b) -> a || b); case "xor" -> binaryBool(stack, (a,b) -> a ^ b);
            case "if_select" -> { Iota no = pop(stack); Iota yes = pop(stack); stack.push(bool(pop(stack)) ? yes : no); }

            case "vec3" -> { double z=number(pop(stack)), y=number(pop(stack)), x=number(pop(stack)); stack.push(Iota.vector(new Vec3(x,y,z))); }
            case "vx" -> stack.push(Iota.number(vector(pop(stack)).x)); case "vy" -> stack.push(Iota.number(vector(pop(stack)).y)); case "vz" -> stack.push(Iota.number(vector(pop(stack)).z));
            case "vadd" -> { Vec3 b=vector(pop(stack)), a=vector(pop(stack)); stack.push(Iota.vector(a.add(b))); }
            case "vsub" -> { Vec3 b=vector(pop(stack)), a=vector(pop(stack)); stack.push(Iota.vector(a.subtract(b))); }
            case "vscale" -> { double scale=number(pop(stack)); stack.push(Iota.vector(vector(pop(stack)).scale(scale))); }
            case "vdot" -> { Vec3 b=vector(pop(stack)), a=vector(pop(stack)); stack.push(Iota.number(a.dot(b))); }
            case "vcross" -> { Vec3 b=vector(pop(stack)), a=vector(pop(stack)); stack.push(Iota.vector(a.cross(b))); }
            case "vlength" -> stack.push(Iota.number(vector(pop(stack)).length()));
            case "vnormalize" -> { Vec3 value=vector(pop(stack)); stack.push(Iota.vector(value.lengthSqr() < 1.0E-12 ? Vec3.ZERO : value.normalize())); }
            case "vdistance" -> { Vec3 b=vector(pop(stack)), a=vector(pop(stack)); stack.push(Iota.number(a.distanceTo(b))); }
            case "vlerp" -> { double t=number(pop(stack)); Vec3 b=vector(pop(stack)), a=vector(pop(stack)); stack.push(Iota.vector(a.lerp(b,t))); }
            case "vrotate_y" -> { double angle=number(pop(stack)); Vec3 v=vector(pop(stack)); double c=Math.cos(angle), s=Math.sin(angle); stack.push(Iota.vector(new Vec3(v.x*c-v.z*s,v.y,v.x*s+v.z*c))); }

            case "target_entity" -> { double distance=bounded(number(pop(stack)),1,96); Entity target=rayEntity(player,distance); if(target==null) throw new Mishap(MishapKind.WORLD,"Сущность не найдена"); stack.push(Iota.entity(target)); }
            case "target_block" -> { double distance=bounded(number(pop(stack)),1,96); HitResult hit=player.pick(distance,0,false); if(!(hit instanceof BlockHitResult blockHit)) throw new Mishap(MishapKind.WORLD,"Блок не найден"); stack.push(Iota.vector(Vec3.atCenterOf(blockHit.getBlockPos()))); }
            case "entities_radius" -> { double radius=bounded(number(pop(stack)),0.5,32); Vec3 center=vector(pop(stack)); List<Iota> values=player.serverLevel().getEntities(player,new AABB(center,center).inflate(radius)).stream().map(Iota::entity).toList(); stack.push(Iota.list(values)); }
            case "entity_pos" -> stack.push(Iota.vector(entity(pop(stack)).position()));
            case "entity_health" -> { LivingEntity living=living(pop(stack)); stack.push(Iota.number(living.getHealth())); }
            case "entity_max_health" -> { LivingEntity living=living(pop(stack)); stack.push(Iota.number(living.getMaxHealth())); }
            case "entity_velocity" -> stack.push(Iota.vector(entity(pop(stack)).getDeltaMovement()));
            case "entity_alive" -> stack.push(Iota.bool(entity(pop(stack)).isAlive()));
            case "entity_type_of" -> { Entity value=entity(pop(stack)); ResourceLocation key=BuiltInRegistries.ENTITY_TYPE.getKey(value.getType()); stack.push(Iota.entityType(key)); }
            case "distance_entities" -> { Entity b=entity(pop(stack)), a=entity(pop(stack)); stack.push(Iota.number(a.distanceTo(b))); }
            case "block_at" -> { BlockPos pos=BlockPos.containing(vector(pop(stack))); stack.push(Iota.block(BuiltInRegistries.BLOCK.getKey(player.serverLevel().getBlockState(pos).getBlock()))); }

            case "blink" -> { double distance=bounded(number(pop(stack)),0,24); Vec3 target=player.position().add(player.getLookAngle().scale(distance)); player.teleportTo(target.x,target.y,target.z); }
            case "teleport" -> { Vec3 position=vector(pop(stack)); Entity target=entity(pop(stack)); requireRange(player,position,96); target.teleportTo(position.x,position.y,position.z); }
            case "impulse" -> { Vec3 impulse=vector(pop(stack)); Entity target=entity(pop(stack)); target.setDeltaMovement(target.getDeltaMovement().add(impulse)); target.hurtMarked=true; }
            case "heal" -> { double amount=bounded(number(pop(stack)),0,40); living(pop(stack)).heal((float)amount); }
            case "damage" -> { double amount=bounded(number(pop(stack)),0,40); entity(pop(stack)).hurt(player.damageSources().generic(),(float)amount); }
            case "ignite" -> { int seconds=(int)bounded(number(pop(stack)),0,60); entity(pop(stack)).setRemainingFireTicks(seconds*20); }
            case "extinguish" -> entity(pop(stack)).clearFire();
            case "lightning" -> { Vec3 position=vector(pop(stack)); requireRange(player,position,64); LightningBolt bolt=EntityType.LIGHTNING_BOLT.create(player.serverLevel()); if(bolt!=null){bolt.moveTo(position);player.serverLevel().addFreshEntity(bolt);} }
            case "explode" -> { double power=bounded(number(pop(stack)),0,8); Vec3 position=vector(pop(stack)); requireRange(player,position,64); player.serverLevel().explode(player,position.x,position.y,position.z,(float)power,Level.ExplosionInteraction.NONE); }
            case "apply_effect" -> applyEffect(stack);
            case "remove_effect" -> removeEffect(stack);
            case "has_effect" -> hasEffect(stack);
            case "effect_level" -> effectLevel(stack);
            case "clear_effects" -> living(pop(stack)).removeAllEffects();
            case "play_sound" -> playSound(player,stack);
            case "summon" -> summon(player,stack);
            case "set_block" -> setBlock(player,stack);
            case "break_block" -> breakBlock(player,stack);
            case "give_item" -> giveItem(stack);
            case "remove_item" -> removeItem(stack);

            case "particle_point" -> particlePoint(player,stack);
            case "particle_line" -> particleLine(player,stack);
            case "particle_circle" -> particleCircle(player,stack);
            case "particle_sphere" -> particleSphere(player,stack);
            case "particle_spiral" -> particleSpiral(player,stack);
            case "particle_cube" -> particleCube(player,stack);
            case "particle_beam" -> particleBeam(player,stack);

            case "barrier_wall" -> barrierWall(player,stack);
            case "barrier_dome" -> barrierDome(player,stack);
            case "barrier_cage" -> barrierCage(player,stack);

            case "eval" -> executeProgram(ctx,program(pop(stack)),depth+1);
            case "if_eval" -> { List<String> no=program(pop(stack)), yes=program(pop(stack)); executeProgram(ctx,bool(pop(stack))?yes:no,depth+1); }
            case "repeat" -> repeat(ctx,stack,depth);
            case "foreach" -> forEach(ctx,stack,depth,false,false);
            case "map" -> forEach(ctx,stack,depth,true,false);
            case "filter" -> forEach(ctx,stack,depth,false,true);
            case "fold" -> fold(ctx,stack,depth);
            case "while_loop" -> whileLoop(ctx,stack,depth);
            case "break_loop" -> throw new BreakSignal();
            case "continue_loop" -> throw new ContinueSignal();
            default -> throw new Mishap(MishapKind.UNKNOWN,"Руна пока не реализована: "+id);
        }
    }

    private static void repeat(Context ctx, Deque<Iota> stack, int depth) {
        List<String> nested=program(pop(stack)); int count=(int)bounded(number(pop(stack)),0,LOOP_LIMIT);
        for(int i=0;i<count;i++) try{executeProgram(ctx,nested,depth+1);}catch(ContinueSignal ignored){}catch(BreakSignal stop){break;}
    }

    private static void forEach(Context ctx, Deque<Iota> stack, int depth, boolean map, boolean filter) {
        List<String> nested=program(pop(stack)); List<Iota> source=list(pop(stack));
        if(source.size()>LOOP_LIMIT) throw new Mishap(MishapKind.LIMIT,"Список слишком велик для цикла");
        List<Iota> result=new ArrayList<>();
        for(Iota value:source){
            stack.push(value);
            try{executeProgram(ctx,nested,depth+1);}catch(ContinueSignal ignored){continue;}catch(BreakSignal stop){break;}
            if(map) result.add(pop(stack));
            else if(filter && bool(pop(stack))) result.add(value);
        }
        if(map||filter) stack.push(Iota.list(result));
    }

    private static void fold(Context ctx, Deque<Iota> stack, int depth) {
        List<String> nested=program(pop(stack)); Iota accumulator=pop(stack); List<Iota> source=list(pop(stack));
        if(source.size()>LOOP_LIMIT) throw new Mishap(MishapKind.LIMIT,"Список слишком велик для свёртки");
        for(Iota value:source){stack.push(accumulator);stack.push(value);executeProgram(ctx,nested,depth+1);accumulator=pop(stack);} stack.push(accumulator);
    }

    private static void whileLoop(Context ctx, Deque<Iota> stack, int depth) {
        List<String> body=program(pop(stack)); List<String> condition=program(pop(stack));
        for(int i=0;i<LOOP_LIMIT;i++){
            executeProgram(ctx,condition,depth+1); if(!bool(pop(stack))) return;
            try{executeProgram(ctx,body,depth+1);}catch(ContinueSignal ignored){}catch(BreakSignal stop){return;}
        }
        throw new Mishap(MishapKind.LIMIT,"Цикл while превысил безопасный предел");
    }

    private static void applyEffect(Deque<Iota> stack){int amplifier=(int)bounded(number(pop(stack)),0,255);int duration=(int)bounded(number(pop(stack)),1,36000);Holder.Reference<MobEffect> effect=effect(pop(stack));living(pop(stack)).addEffect(new MobEffectInstance(effect,duration,amplifier));}
    private static void removeEffect(Deque<Iota> stack){Holder.Reference<MobEffect> effect=effect(pop(stack));living(pop(stack)).removeEffect(effect);}
    private static void hasEffect(Deque<Iota> stack){Holder.Reference<MobEffect> effect=effect(pop(stack));stack.push(Iota.bool(living(pop(stack)).hasEffect(effect)));}
    private static void effectLevel(Deque<Iota> stack){Holder.Reference<MobEffect> effect=effect(pop(stack));MobEffectInstance instance=living(pop(stack)).getEffect(effect);stack.push(Iota.number(instance==null?-1:instance.getAmplifier()));}

    private static void playSound(ServerPlayer player,Deque<Iota> stack){double pitch=bounded(number(pop(stack)),0.25,4);double volume=bounded(number(pop(stack)),0,8);ResourceLocation id=resource(pop(stack),Iota.Kind.SOUND);Vec3 pos=vector(pop(stack));SoundEvent sound=BuiltInRegistries.SOUND_EVENT.get(id);if(sound==null)throw new Mishap(MishapKind.WORLD,"Звук не найден");player.serverLevel().playSound(null,BlockPos.containing(pos),sound,SoundSource.PLAYERS,(float)volume,(float)pitch);}
    private static void summon(ServerPlayer player,Deque<Iota> stack){ResourceLocation id=resource(pop(stack),Iota.Kind.ENTITY_TYPE);Vec3 pos=vector(pop(stack));requireRange(player,pos,48);EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.get(id);if(type==null||type==EntityType.PLAYER)throw new Mishap(MishapKind.WORLD,"Этот тип сущности нельзя призвать");Entity created=type.create(player.serverLevel());if(created==null)throw new Mishap(MishapKind.WORLD,"Сущность не создаётся");created.moveTo(pos);player.serverLevel().addFreshEntity(created);stack.push(Iota.entity(created));}
    private static void setBlock(ServerPlayer player,Deque<Iota> stack){ResourceLocation id=resource(pop(stack),Iota.Kind.BLOCK);Vec3 vec=vector(pop(stack));requireRange(player,vec,WORLD_EDIT_RANGE);Block block=BuiltInRegistries.BLOCK.get(id);if(block==null)throw new Mishap(MishapKind.WORLD,"Блок не найден");player.serverLevel().setBlockAndUpdate(BlockPos.containing(vec),block.defaultBlockState());}
    private static void breakBlock(ServerPlayer player,Deque<Iota> stack){Vec3 vec=vector(pop(stack));requireRange(player,vec,WORLD_EDIT_RANGE);player.serverLevel().destroyBlock(BlockPos.containing(vec),true,player);}
    private static void giveItem(Deque<Iota> stack){int count=(int)bounded(number(pop(stack)),1,64);ResourceLocation id=resource(pop(stack),Iota.Kind.ITEM);Entity target=entity(pop(stack));if(!(target instanceof ServerPlayer player))throw new Mishap(MishapKind.TYPE,"Предметы можно выдавать только игрокам");Item item=BuiltInRegistries.ITEM.get(id);ItemStack output=new ItemStack(item,count);if(!player.getInventory().add(output))player.drop(output,false);}
    private static void removeItem(Deque<Iota> stack){int remaining=(int)bounded(number(pop(stack)),1,4096);ResourceLocation id=resource(pop(stack),Iota.Kind.ITEM);Entity target=entity(pop(stack));if(!(target instanceof ServerPlayer player))throw new Mishap(MishapKind.TYPE,"Инвентарь доступен только у игроков");Item item=BuiltInRegistries.ITEM.get(id);for(int slot=0;slot<player.getInventory().getContainerSize()&&remaining>0;slot++){ItemStack found=player.getInventory().getItem(slot);if(found.is(item)){int take=Math.min(remaining,found.getCount());found.shrink(take);remaining-=take;}}stack.push(Iota.number(remaining));}

    private static void particlePoint(ServerPlayer player,Deque<Iota> stack){int count=(int)bounded(number(pop(stack)),1,500);ParticleOptions particle=particle(pop(stack));Vec3 pos=vector(pop(stack));player.serverLevel().sendParticles(particle,pos.x,pos.y,pos.z,count,.05,.05,.05,.01);}
    private static void particleLine(ServerPlayer player,Deque<Iota> stack){int points=(int)bounded(number(pop(stack)),2,300);ParticleOptions particle=particle(pop(stack));Vec3 end=vector(pop(stack)),start=vector(pop(stack));for(int i=0;i<points;i++){Vec3 p=start.lerp(end,i/(double)(points-1));particle(player,particle,p);}}
    private static void particleCircle(ServerPlayer player,Deque<Iota> stack){int points=(int)bounded(number(pop(stack)),8,360);ParticleOptions particle=particle(pop(stack));double radius=bounded(number(pop(stack)),.1,32);Vec3 center=vector(pop(stack));for(int i=0;i<points;i++){double a=Math.PI*2*i/points;particle(player,particle,center.add(Math.cos(a)*radius,0,Math.sin(a)*radius));}}
    private static void particleSphere(ServerPlayer player,Deque<Iota> stack){int points=(int)bounded(number(pop(stack)),12,600);ParticleOptions particle=particle(pop(stack));double radius=bounded(number(pop(stack)),.1,24);Vec3 center=vector(pop(stack));double golden=Math.PI*(3-Math.sqrt(5));for(int i=0;i<points;i++){double y=1-(i/(double)(points-1))*2,r=Math.sqrt(Math.max(0,1-y*y)),theta=golden*i;particle(player,particle,center.add(Math.cos(theta)*r*radius,y*radius,Math.sin(theta)*r*radius));}}
    private static void particleSpiral(ServerPlayer player,Deque<Iota> stack){int points=(int)bounded(number(pop(stack)),12,500);ParticleOptions particle=particle(pop(stack));double height=bounded(number(pop(stack)),.1,32),radius=bounded(number(pop(stack)),.1,16);Vec3 center=vector(pop(stack));for(int i=0;i<points;i++){double t=i/(double)(points-1),a=t*Math.PI*8;particle(player,particle,center.add(Math.cos(a)*radius,t*height,Math.sin(a)*radius));}}
    private static void particleCube(ServerPlayer player,Deque<Iota> stack){int points=(int)bounded(number(pop(stack)),2,64);ParticleOptions particle=particle(pop(stack));double size=bounded(number(pop(stack)),.1,32);Vec3 center=vector(pop(stack));double h=size/2;for(int axis=0;axis<3;axis++)for(int a=-1;a<=1;a+=2)for(int b=-1;b<=1;b+=2)for(int i=0;i<points;i++){double t=-h+size*i/(points-1.0);Vec3 p=switch(axis){case 0->center.add(t,a*h,b*h);case 1->center.add(a*h,t,b*h);default->center.add(a*h,b*h,t);};particle(player,particle,p);}}
    private static void particleBeam(ServerPlayer player,Deque<Iota> stack){double width=bounded(number(pop(stack)),.01,2);int points=(int)bounded(number(pop(stack)),2,500);ParticleOptions particle=particle(pop(stack));Vec3 end=vector(pop(stack)),start=vector(pop(stack));Vec3 direction=end.subtract(start).normalize();Vec3 side=direction.cross(new Vec3(0,1,0));if(side.lengthSqr()<1.0E-8)side=new Vec3(1,0,0);side=side.normalize().scale(width);for(int i=0;i<points;i++){double t=i/(double)(points-1);Vec3 center=start.lerp(end,t);particle(player,particle,center.add(side.scale(Math.sin(t*Math.PI*12))));}}
    private static void particle(ServerPlayer player,ParticleOptions type,Vec3 pos){player.serverLevel().sendParticles(type,pos.x,pos.y,pos.z,1,0,0,0,0);}

    private static void barrierWall(ServerPlayer player,Deque<Iota> stack){int ticks=(int)bounded(number(pop(stack)),20,2400),height=(int)bounded(number(pop(stack)),1,16);Vec3 end=vector(pop(stack)),start=vector(pop(stack));int length=(int)Math.ceil(start.distanceTo(end));for(int i=0;i<=length;i++){Vec3 p=start.lerp(end,length==0?0:i/(double)length);for(int y=0;y<height;y++)placeBarrier(player.serverLevel(),BlockPos.containing(p.x,p.y+y,p.z),ticks);}}
    private static void barrierDome(ServerPlayer player,Deque<Iota> stack){int ticks=(int)bounded(number(pop(stack)),20,2400),radius=(int)bounded(number(pop(stack)),1,12);Vec3 center=vector(pop(stack));BlockPos c=BlockPos.containing(center);for(int x=-radius;x<=radius;x++)for(int y=0;y<=radius;y++)for(int z=-radius;z<=radius;z++){double d=Math.sqrt(x*x+y*y+z*z);if(d>=radius-.7&&d<=radius+.4)placeBarrier(player.serverLevel(),c.offset(x,y,z),ticks);}}
    private static void barrierCage(ServerPlayer player,Deque<Iota> stack){int ticks=(int)bounded(number(pop(stack)),20,2400),height=(int)bounded(number(pop(stack)),1,16),radius=(int)bounded(number(pop(stack)),1,12);BlockPos c=BlockPos.containing(vector(pop(stack)));for(int x=-radius;x<=radius;x++)for(int y=0;y<=height;y++)for(int z=-radius;z<=radius;z++)if(Math.abs(x)==radius||Math.abs(z)==radius||y==0||y==height)placeBarrier(player.serverLevel(),c.offset(x,y,z),ticks);}
    private static void placeBarrier(ServerLevel level,BlockPos pos,int ticks){BlockState existing=level.getBlockState(pos);if(!existing.canBeReplaced())return;level.setBlock(pos,ModBlocks.ARCANE_BARRIER.get().defaultBlockState(),3);level.scheduleTick(pos,ModBlocks.ARCANE_BARRIER.get(),ticks);}

    private static Holder.Reference<MobEffect> effect(Iota iota){ResourceLocation id=resource(iota,Iota.Kind.EFFECT);return BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(ResourceKey.create(Registries.MOB_EFFECT,id));}
    private static ParticleOptions particle(Iota iota){ResourceLocation id=resource(iota,Iota.Kind.PARTICLE);var type=BuiltInRegistries.PARTICLE_TYPE.get(id);if(type instanceof SimpleParticleType simple)return simple;throw new Mishap(MishapKind.TYPE,"Частица требует параметров и пока не поддерживается");}
    private static Entity rayEntity(ServerPlayer player,double distance){Vec3 start=player.getEyePosition(),end=start.add(player.getLookAngle().scale(distance));AABB box=player.getBoundingBox().expandTowards(player.getLookAngle().scale(distance)).inflate(1);Entity best=null;double bestDistance=distance*distance;for(Entity entity:player.serverLevel().getEntities(player,box,Entity::isPickable)){var hit=entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start,end);if(hit.isPresent()){double d=start.distanceToSqr(hit.get());if(d<bestDistance){bestDistance=d;best=entity;}}}return best;}
    private static void requireRange(ServerPlayer player,Vec3 position,double max){if(player.position().distanceToSqr(position)>max*max)throw new Mishap(MishapKind.WORLD,"Цель слишком далеко");}

    private static Iota require(Deque<Iota> stack,String message){Iota value=stack.peek();if(value==null)throw new Mishap(MishapKind.STACK,message);return value;}
    private static Iota pop(Deque<Iota> stack){Iota value=stack.poll();if(value==null)throw new Mishap(MishapKind.STACK,"Пустой стек");return value;}
    private static double number(Iota iota){if(iota.kind()!=Iota.Kind.NUMBER)throw new Mishap(MishapKind.TYPE,"Ожидалось число, получено "+iota.kind());return(double)iota.value();}
    private static boolean bool(Iota iota){if(iota.kind()!=Iota.Kind.BOOLEAN)throw new Mishap(MishapKind.TYPE,"Ожидалась логика, получено "+iota.kind());return(boolean)iota.value();}
    private static Vec3 vector(Iota iota){if(iota.kind()!=Iota.Kind.VECTOR)throw new Mishap(MishapKind.TYPE,"Ожидался вектор, получено "+iota.kind());return(Vec3)iota.value();}
    private static Entity entity(Iota iota){if(iota.kind()!=Iota.Kind.ENTITY)throw new Mishap(MishapKind.TYPE,"Ожидалась сущность, получено "+iota.kind());return(Entity)iota.value();}
    private static LivingEntity living(Iota iota){Entity entity=entity(iota);if(!(entity instanceof LivingEntity living))throw new Mishap(MishapKind.TYPE,"Сущность не является живой");return living;}
    @SuppressWarnings("unchecked")private static List<Iota> list(Iota iota){if(iota.kind()!=Iota.Kind.LIST)throw new Mishap(MishapKind.TYPE,"Ожидался список, получено "+iota.kind());return(List<Iota>)iota.value();}
    @SuppressWarnings("unchecked")private static List<String> program(Iota iota){if(iota.kind()!=Iota.Kind.PROGRAM)throw new Mishap(MishapKind.TYPE,"Ожидалась программа, получено "+iota.kind());return(List<String>)iota.value();}
    private static ResourceLocation resource(Iota iota,Iota.Kind kind){if(iota.kind()!=kind)throw new Mishap(MishapKind.TYPE,"Ожидался тип "+kind+", получено "+iota.kind());return(ResourceLocation)iota.value();}
    private static double bounded(double value,double min,double max){if(!Double.isFinite(value))throw new Mishap(MishapKind.VALUE,"Некорректное число");return Math.max(min,Math.min(max,value));}
    private static void unaryNumber(Deque<Iota> stack,DoubleUnary op){stack.push(Iota.number(op.apply(number(pop(stack)))));}
    private static void binaryNumber(Deque<Iota> stack,DoubleBinary op){double b=number(pop(stack)),a=number(pop(stack));stack.push(Iota.number(op.apply(a,b)));}
    private static void compare(Deque<Iota> stack,DoubleCompare op){double b=number(pop(stack)),a=number(pop(stack));stack.push(Iota.bool(op.apply(a,b)));}
    private static void binaryBool(Deque<Iota> stack,BoolBinary op){boolean b=bool(pop(stack)),a=bool(pop(stack));stack.push(Iota.bool(op.apply(a,b)));}

    private static void showMishap(ServerPlayer player,Mishap mishap){var particle=switch(mishap.kind){case STACK->ParticleTypes.SMOKE;case TYPE->ParticleTypes.ENCHANT;case MEDIA->ParticleTypes.WITCH;case LIMIT->ParticleTypes.ANGRY_VILLAGER;case WORLD->ParticleTypes.PORTAL;case VALUE->ParticleTypes.CRIT;case CONTROL->ParticleTypes.REVERSE_PORTAL;case UNKNOWN->ParticleTypes.ELECTRIC_SPARK;};player.displayClientMessage(Component.literal("§cСбой ["+mishap.kind+"]: "+mishap.getMessage()),true);player.serverLevel().sendParticles(particle,player.getX(),player.getEyeY(),player.getZ(),24,.5,.5,.5,.02);}

    private static final class Context{final ServerPlayer player;final ItemStack staff;final Deque<Iota> stack=new ArrayDeque<>();int operations;Context(ServerPlayer player,ItemStack staff){this.player=player;this.staff=staff;}}
    private enum MishapKind{STACK,TYPE,MEDIA,LIMIT,WORLD,VALUE,CONTROL,UNKNOWN}
    private static final class Mishap extends RuntimeException{final MishapKind kind;Mishap(MishapKind kind,String message){super(message);this.kind=kind;}}
    private static final class HaltSignal extends RuntimeException{}
    private static final class BreakSignal extends RuntimeException{}
    private static final class ContinueSignal extends RuntimeException{}
    @FunctionalInterface private interface DoubleUnary{double apply(double value);}
    @FunctionalInterface private interface DoubleBinary{double apply(double a,double b);}
    @FunctionalInterface private interface DoubleCompare{boolean apply(double a,double b);}
    @FunctionalInterface private interface BoolBinary{boolean apply(boolean a,boolean b);}
    public record Result(boolean success,String message){public static Result ok(String message){return new Result(true,message);}public static Result fail(String message){return new Result(false,message);}}
}
