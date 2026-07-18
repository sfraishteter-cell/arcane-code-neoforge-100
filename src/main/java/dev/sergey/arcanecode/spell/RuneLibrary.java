package dev.sergey.arcanecode.spell;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Complete live rune catalogue. Core language runes are followed by generated
 * registry runes for effects, particles, blocks, items, sounds and entity types.
 */
public final class RuneLibrary {
    private static volatile List<RuneDefinition> all;
    private static volatile Map<String, RuneDefinition> byId;
    private static volatile Map<String, RuneDefinition> byPattern;

    private RuneLibrary() {}

    public static List<RuneDefinition> all() { ensureBuilt(); return all; }
    public static RuneDefinition byId(String id) { ensureBuilt(); return byId.get(id); }
    public static RuneDefinition byPattern(String key) { ensureBuilt(); return byPattern.get(key); }

    private static synchronized void ensureBuilt() {
        if (all != null) return;
        List<Spec> specs = new ArrayList<>();

        many(specs, "constants", 0, "— → значение",
            "zero","one","two","three","four","five","six","seven","eight","nine","ten","hundred","pi","e","true","false","null");
        many(specs, "vectors", 0, "— → вектор", "self_pos","eye_pos","look_vec","up","down");
        add(specs,"self","entities",0,"— → сущность");
        add(specs,"world_time","world",0,"— → число");
        add(specs,"is_raining","world",0,"— → логика");

        many(specs,"stack",0,"стек → стек","dup","swap","drop","over","rot","clear","depth","pack2","pack3","unpack");
        add(specs,"list_size","lists",0,"список → число");
        add(specs,"list_get","lists",1,"список индекс → значение");
        add(specs,"list_append","lists",1,"список значение → список");
        add(specs,"list_concat","lists",1,"список список → список");
        add(specs,"list_reverse","lists",1,"список → список");

        many(specs,"math",1,"a b → число","add","sub","mul","div","mod","pow","min","max","atan2");
        many(specs,"math",1,"a → число","sqrt","abs","neg","floor","ceil","round","sin","cos","tan","log","exp");
        add(specs,"clamp","math",1,"x min max → число");
        add(specs,"lerp","math",1,"a b t → число");
        add(specs,"random","math",2,"min max → число");

        many(specs,"logic",1,"a b → логика","eq","neq","lt","lte","gt","gte","and","or","xor");
        add(specs,"not","logic",1,"логика → логика");
        add(specs,"if_select","logic",2,"условие да нет → значение");

        many(specs,"vectors",1,"значения → вектор/число","vec3","vadd","vsub","vscale","vdot","vcross","vdistance");
        many(specs,"vectors",1,"вектор → значение","vx","vy","vz","vlength","vnormalize");
        add(specs,"vlerp","vectors",1,"a b t → вектор");
        add(specs,"vrotate_y","vectors",1,"вектор угол → вектор");

        add(specs,"target_entity","senses",8,"дальность → сущность");
        add(specs,"target_block","senses",5,"дальность → вектор");
        add(specs,"entities_radius","senses",12,"центр радиус → список");
        add(specs,"entity_pos","senses",1,"сущность → вектор");
        add(specs,"entity_health","senses",1,"сущность → число");
        add(specs,"entity_max_health","senses",1,"сущность → число");
        add(specs,"entity_velocity","senses",1,"сущность → вектор");
        add(specs,"entity_alive","senses",1,"сущность → логика");
        add(specs,"entity_type_of","senses",1,"сущность → тип сущности");
        add(specs,"distance_entities","senses",1,"сущность сущность → число");
        add(specs,"block_at","senses",1,"позиция → блок");

        add(specs,"blink","actions",30,"дальность → —");
        add(specs,"teleport","actions",45,"сущность позиция → —");
        add(specs,"impulse","actions",20,"сущность вектор → —");
        add(specs,"heal","actions",20,"сущность число → —");
        add(specs,"damage","actions",25,"сущность число → —");
        add(specs,"ignite","actions",18,"сущность секунды → —");
        add(specs,"extinguish","actions",8,"сущность → —");
        add(specs,"lightning","actions",80,"позиция → —");
        add(specs,"explode","actions",90,"позиция сила → —");
        add(specs,"summon","actions",90,"позиция тип сущности → сущность");
        add(specs,"play_sound","actions",6,"позиция звук громкость высота → —");
        add(specs,"set_block","world",50,"позиция блок → —");
        add(specs,"break_block","world",35,"позиция → —");
        add(specs,"give_item","items",60,"игрок предмет количество → —");
        add(specs,"remove_item","items",15,"игрок предмет количество → остаток");

        add(specs,"apply_effect","effects",20,"сущность эффект тики уровень → —");
        add(specs,"remove_effect","effects",8,"сущность эффект → —");
        add(specs,"has_effect","effects",1,"сущность эффект → логика");
        add(specs,"effect_level","effects",1,"сущность эффект → число");
        add(specs,"clear_effects","effects",12,"сущность → —");

        add(specs,"particle_point","visuals",2,"позиция частица количество → —");
        add(specs,"particle_line","visuals",5,"начало конец частица точки → —");
        add(specs,"particle_circle","visuals",8,"центр радиус частица точки → —");
        add(specs,"particle_sphere","visuals",15,"центр радиус частица точки → —");
        add(specs,"particle_spiral","visuals",15,"центр радиус высота частица точки → —");
        add(specs,"particle_cube","visuals",14,"центр размер частица точки → —");
        add(specs,"particle_beam","visuals",12,"начало конец частица точки ширина → —");

        add(specs,"barrier_wall","barriers",70,"начало конец высота тики → —");
        add(specs,"barrier_dome","barriers",120,"центр радиус тики → —");
        add(specs,"barrier_cage","barriers",100,"центр радиус высота тики → —");

        add(specs,"quote_next","control",0,"следующая руна → программа");
        add(specs,"quote_n","control",0,"число + следующие руны → программа");
        add(specs,"eval","control",2,"программа → выполнение");
        add(specs,"if_eval","control",3,"условие программа программа → выполнение");
        add(specs,"repeat","control",3,"число программа → выполнение");
        add(specs,"foreach","control",4,"список программа → выполнение");
        add(specs,"map","control",5,"список программа → список");
        add(specs,"filter","control",5,"список программа → список");
        add(specs,"fold","control",6,"список начало программа → значение");
        add(specs,"while_loop","control",8,"условие программа → выполнение");
        add(specs,"break_loop","control",0,"— → выход из цикла");
        add(specs,"continue_loop","control",0,"— → следующая итерация");
        add(specs,"halt","control",0,"— → остановка");

        addRegistryRunes(specs);

        List<int[]> patterns = generatePatterns(specs.size());
        List<RuneDefinition> built = new ArrayList<>(specs.size());
        Map<String,RuneDefinition> ids = new LinkedHashMap<>();
        Map<String,RuneDefinition> keys = new HashMap<>();
        for(int index=0;index<specs.size();index++){
            Spec spec=specs.get(index);
            Component name=dynamicName(spec);
            Component description=dynamicDescription(spec);
            RuneDefinition definition=new RuneDefinition(spec.id,spec.category,spec.cost,spec.signature,name,description,patterns.get(index),spec.argument);
            built.add(definition);ids.put(definition.id(),definition);keys.put(PatternNormalizer.key(definition.directions()),definition);
        }
        all=List.copyOf(built);byId=Map.copyOf(ids);byPattern=Map.copyOf(keys);
    }

    private static void addRegistryRunes(List<Spec> specs){
        BuiltInRegistries.MOB_EFFECT.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(id->specs.add(new Spec("effect/"+id,"effects",0,"— → эффект",id.toString())));
        BuiltInRegistries.PARTICLE_TYPE.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(id->{if(BuiltInRegistries.PARTICLE_TYPE.get(id) instanceof SimpleParticleType)specs.add(new Spec("particle/"+id,"visuals",0,"— → частица",id.toString()));});
        BuiltInRegistries.BLOCK.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(id->specs.add(new Spec("block/"+id,"world",0,"— → блок",id.toString())));
        BuiltInRegistries.ITEM.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(id->specs.add(new Spec("item/"+id,"items",0,"— → предмет",id.toString())));
        BuiltInRegistries.SOUND_EVENT.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(id->specs.add(new Spec("sound/"+id,"sounds",0,"— → звук",id.toString())));
        BuiltInRegistries.ENTITY_TYPE.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(id->specs.add(new Spec("entity_type/"+id,"entities",0,"— → тип сущности",id.toString())));
    }

    private static Component dynamicName(Spec spec){
        if(spec.id.startsWith("effect/")){var value=BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(spec.argument));return value==null?Component.literal("Эффект: "+spec.argument):Component.literal("Эффект: ").append(Component.translatable(value.getDescriptionId()));}
        if(spec.id.startsWith("block/")){var value=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(spec.argument));return Component.literal("Блок: ").append(Component.translatable(value.getDescriptionId()));}
        if(spec.id.startsWith("item/")){var value=BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.argument));return Component.literal("Предмет: ").append(Component.translatable(value.getDescriptionId()));}
        if(spec.id.startsWith("entity_type/")){var value=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(spec.argument));return Component.literal("Сущность: ").append(Component.translatable(value.getDescriptionId()));}
        if(spec.id.startsWith("particle/"))return Component.literal("Частица: "+spec.argument);
        if(spec.id.startsWith("sound/"))return Component.literal("Звук: "+spec.argument);
        return Component.translatable("rune.arcanecode."+spec.id);
    }

    private static Component dynamicDescription(Spec spec){
        if(spec.argument.isBlank())return Component.translatable("rune.arcanecode."+spec.id+".desc");
        return Component.literal("Помещает в стек значение реестра "+spec.argument+".");
    }

    private static void many(List<Spec> specs,String category,int cost,String signature,String... ids){for(String id:ids)add(specs,id,category,cost,signature);}
    private static void add(List<Spec> specs,String id,String category,int cost,String signature){specs.add(new Spec(id,category,cost,signature,""));}

    private static List<int[]> generatePatterns(int count){
        List<int[]> result=new ArrayList<>(count);
        for(int length=2;result.size()<count&&length<=16;length++){int[] path=new int[length];path[0]=0;generateRecursive(result,path,1,count,new HashSet<>(),0,0);}
        if(result.size()<count)throw new IllegalStateException("Not enough unique rune patterns: "+result.size()+" / "+count);
        return result;
    }

    private static void generateRecursive(List<int[]> out,int[] path,int index,int limit,Set<String> usedEdges,int q,int r){
        if(out.size()>=limit)return;
        if(index==path.length){out.add(path.clone());return;}
        int previous=path[index-1];
        for(int direction=0;direction<6;direction++){
            if(direction==Math.floorMod(previous+3,6))continue;
            int nq=q+DQ[direction],nr=r+DR[direction];
            if(hexDistance(nq,nr)>7)continue;
            String edge=q+":"+r+">"+nq+":"+nr,reverse=nq+":"+nr+">"+q+":"+r;
            if(usedEdges.contains(edge)||usedEdges.contains(reverse))continue;
            path[index]=direction;usedEdges.add(edge);generateRecursive(out,path,index+1,limit,usedEdges,nq,nr);usedEdges.remove(edge);
            if(out.size()>=limit)return;
        }
    }

    private static int hexDistance(int q,int r){return Math.max(Math.max(Math.abs(q),Math.abs(r)),Math.abs(q+r));}
    private static final int[] DQ={1,1,0,-1,-1,0};
    private static final int[] DR={0,-1,-1,0,1,1};
    private record Spec(String id,String category,int cost,String signature,String argument){}
}
