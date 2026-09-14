package com.tankecho.zensudoku;

/** Semantic colors shared by all screens, dialogs and the animated board. */
public final class Palette {
    public static final String[] NAMES={"白色简约","深色简约","浅蓝","浅红","原野绿"};
    public final int id;public final boolean dark;
    public final int bg,surface,ink,muted,accent,onAccent,pale,hero,onHero,heroMuted,lime,line,peer,match,selected,grid,error,errorBg,fx,fxAlt;
    private Palette(int id,boolean dark,int bg,int surface,int ink,int muted,int accent,int onAccent,int pale,int hero,int lime,int line,int peer,int match,int selected,int grid,int error,int errorBg,int fx,int fxAlt){
        this.id=id;this.dark=dark;this.bg=bg;this.surface=surface;this.ink=ink;this.muted=muted;this.accent=accent;this.onAccent=onAccent;this.pale=pale;this.hero=hero;this.onHero=0xfff6fafc;this.heroMuted=0xffcad6dc;this.lime=lime;this.line=line;this.peer=peer;this.match=match;this.selected=selected;this.grid=grid;this.error=error;this.errorBg=errorBg;this.fx=fx;this.fxAlt=fxAlt;
    }
    public static Palette of(int id){switch(id){
        case 0:return new Palette(0,false,0xfff6f7f9,0xffffffff,0xff202936,0xff646d7b,0xff35465c,0xffffffff,0xffe9edf2,0xff263342,0xffd8e5f1,0xffdce1e8,0xfff0f3f7,0xffcddbea,0xffdce5ef,0xff92a0b0,0xffaa443e,0xffffe1de,0xff477db4,0xff926fc8);
        case 1:return new Palette(1,true,0xff10161e,0xff19232e,0xffe7f0f9,0xffa1b0bf,0xff9bdbe9,0xff102632,0xff233441,0xff1b2e3e,0xffbbd9fa,0xff344551,0xff20303e,0xff385d72,0xff2c4a5e,0xff668899,0xffffa395,0xff533a41,0xff88e5f8,0xffd5a7ff);
        case 2:return new Palette(2,false,0xfff0f7fd,0xfffcfeff,0xff234461,0xff587187,0xff246995,0xffffffff,0xffe0effa,0xff204967,0xffbfe5f9,0xffd0e1ed,0xffebf4fb,0xffc5e3f6,0xffd2e8f7,0xff83afc9,0xffaa554e,0xffffe4df,0xff3b94ce,0xff8475d6);
        case 3:return new Palette(3,false,0xfffcf3f3,0xfffffcfc,0xff623d43,0xff82666b,0xffa04658,0xffffffff,0xfff5e3e7,0xff643340,0xffffd9db,0xffead5da,0xfffaf0f2,0xfff1cbd4,0xfff5dbe1,0xffc1949e,0xffab4035,0xffffddd6,0xffd06b88,0xffac81ce);
        default:return new Palette(4,false,0xfff7f8f4,0xfffdfefa,0xff213d33,0xff617165,0xff267768,0xfffdfefa,0xffe9f0e5,0xff183e35,0xffd1e5a7,0xffe0e6db,0xffedf2eb,0xffc1decf,0xffd6e6d7,0xff94ada2,0xffb65343,0xffffe1d8,0xff39a38e,0xff91b857);
    }}
}
