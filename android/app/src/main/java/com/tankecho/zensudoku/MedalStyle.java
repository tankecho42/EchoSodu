package com.tankecho.zensudoku;

/** Stable visual identity, independent of progress and storage. */
final class MedalStyle {
    final int motif,rank,base,light,metal;final String title,mark;
    private MedalStyle(int motif,int rank,int base,int light,int metal,String title,String mark){this.motif=motif;this.rank=rank;this.base=base;this.light=light;this.metal=metal;this.title=title;this.mark=mark;}
    static MedalStyle of(Medal m){
        switch(m.id){
        case "journey-1":return s(0,0,0xff795947,0xfff4d8ac,0xffb7865e,"砂岩铜 · 初航罗盘","1");
        case "journey-10":return s(1,1,0xff285880,0xffb6e4ff,0xffbdcfe2,"冰川银 · 星轨航线","10");
        case "journey-50":return s(2,2,0xff236b56,0xffd2edb3,0xffe2bd64,"翡翠金 · 群山桂冠","50");
        case "journey-100":return s(3,3,0xff554175,0xffeac7ff,0xffffd988,"极光金 · 百次星冠","100");
        case "insight-independent":return s(4,0,0xff806f52,0xffffebbd,0xffc49a67,"羊皮铜 · 自由羽笔","I");
        case "insight-perfect":return s(5,1,0xff2a7893,0xffceffff,0xffc5ebf2,"霜晶银 · 无瑕晶石","0");
        case "insight-expert":return s(6,2,0xff8a3947,0xffffcec0,0xffffcf78,"绯红金 · 极地之巅","EX");
        case "insight-all":return s(7,3,0xff205d6a,0xffb0ffe5,0xffffe0a1,"幻彩金 · 四境星盘","4");
        case "rhythm-3":return s(8,0,0xff607257,0xffe0ebc2,0xffc2a276,"苔原铜 · 三叶约定","3");
        case "rhythm-7":return s(9,1,0xff386959,0xffbfe5d0,0xffcfddc8,"森林银 · 七日花环","7");
        case "rhythm-30":return s(10,2,0xff474c83,0xffd9d8ff,0xffe8cf80,"月辉金 · 三十夜守候","30");
        case "rhythm-daily":return s(11,3,0xff916020,0xffffeeb8,0xffffd484,"日耀金 · 日历宝匣","7D");
        case "focus-first":return s(12,0,0xff956858,0xffffdfc4,0xffc69878,"暖陶铜 · 一点新芽","1");
        case "focus-ten":return s(13,1,0xff8a535c,0xffffd3d5,0xffe0b1a1,"玫瑰银 · 十瓣绽放","10");
        case "focus-hour":return s(14,2,0xff326d84,0xffc8f3fa,0xffdbd398,"海蓝金 · 静谧沙漏","1H");
        case "focus-ten-hours":return s(15,3,0xff614d83,0xffefcbff,0xffffd998,"暮光金 · 永恒时轮","10H");
        default:throw new IllegalArgumentException("Unknown medal identity: "+m.id);
        }
    }
    private static MedalStyle s(int motif,int rank,int base,int light,int metal,String title,String mark){return new MedalStyle(motif,rank,base,light,metal,title,mark);}
    String rankName(){return new String[]{"初阶","进阶","珍稀","典藏"}[rank];}
}
