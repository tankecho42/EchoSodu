package com.tankecho.zensudoku;

import java.util.*;

public final class Medal {
    public static final String[] FAMILIES={"旅程","推理","节奏","专注"};
    public final String id,name,condition,unit;
    public final int family,tier;
    public final long value,target;
    private Medal(String id,String name,String condition,String unit,int family,int tier,long value,long target){this.id=id;this.name=name;this.condition=condition;this.unit=unit;this.family=family;this.tier=tier;this.value=Math.max(0,value);this.target=target;}
    public boolean unlocked(){return value>=target;}
    public float fraction(){return Math.min(1f,(float)value/target);}
    public String progress(){return Math.min(value,target)+" / "+target+" "+unit;}
    public static List<Medal> all(ProgressStats s){
        List<Medal> m=new ArrayList<>();
        String[] journey={"初次相遇","十格远行","熟悉的风景","百次回响"};int[] wins={1,10,50,100};
        for(int i=0;i<4;i++)m.add(new Medal("journey-"+wins[i],journey[i],"累计完成 "+wins[i]+" 局挑战（含每日挑战，练习另计）。","局",0,i,s.challengeWins,wins[i]));
        m.add(new Medal("insight-independent","独立的光","完成 1 局无提示挑战。","局",1,0,s.noHintWins,1));
        m.add(new Medal("insight-perfect","无瑕一页","完成 1 局无提示且零错误的挑战。","局",1,1,s.perfectWins,1));
        m.add(new Medal("insight-expert","极地解谜家","完成 1 局专家难度挑战。","局",1,2,s.levelWins[3],1));
        m.add(new Medal("insight-all","四境漫游","简单、中等、困难、专家各完成至少 1 局挑战。","种",1,3,s.completedDifficulties(),4));
        String[] rhythm={"三日约定","一周相伴","月光守候"};int[] streak={3,7,30};
        for(int i=0;i<3;i++)m.add(new Medal("rhythm-"+streak[i],rhythm[i],"曾连续 "+streak[i]+" 天游玩，每天至少 1 秒；中断后仍保留。","天",2,i,s.longestStreak,streak[i]));
        m.add(new Medal("rhythm-daily","日历收藏家","完成 7 个不同日期的每日挑战，补做也计入。","天",2,3,s.dailyWins,7));
        m.add(new Medal("focus-first","慢慢来也好","完成 1 局不限错练习。","局",3,0,s.practiceWins,1));
        m.add(new Medal("focus-ten","练习的力量","累计完成 10 局不限错练习。","局",3,1,s.practiceWins,10));
        m.add(new Medal("focus-hour","一小时宁静","累计实际游玩 60 分钟，暂停和后台不计时。","分钟",3,2,s.totalTime/60000,60));
        m.add(new Medal("focus-ten-hours","时间的朋友","累计实际游玩 600 分钟。","分钟",3,3,s.totalTime/60000,600));
        return Collections.unmodifiableList(m);
    }
}
