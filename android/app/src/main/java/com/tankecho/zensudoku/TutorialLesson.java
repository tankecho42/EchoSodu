package com.tankecho.zensudoku;

/** Deterministic learning boards, entirely separate from player progress. */
public final class TutorialLesson {
    public static final String SOLUTION="534678912672195348198342567859761423426853791713924856961537284287419635345286179";
    public static final String[] TITLES={"一行、一列、一座宫","先选格，再填数","一个数字，一路填下去","给推理留一点笔记","让 Echo 陪你想一步","每一份完成，都有回响"};
    public static final String[] LABELS={"数独的三个规则","常规填数","快速模式","候选数与笔记","提示与推理","完成与庆祝"};
    public static final String[] DESCRIPTIONS={
        "把 1–9 放进每一行、每一列和每个 3×3 宫。同一区域内，数字不能重复。",
        "常规模式先选择空格，再按底部数字。深色是题目数字，主题色是你填入的答案。",
        "快速模式先选择数字，再连续点击空格。同数会一起高亮，填满 9 个后数字键自动取消并禁用。",
        "候选数列出当前合法的可能性；笔记由你手动记录。笔记不会消耗错误次数。",
        "提示先解释推理，再由你决定是否填入。每局最多 3 次，查看新提示就会计次。",
        "一行、一列、一宫或同一数字完成时，相关格子会起伏。整盘完成还有全屏庆祝。"};
    public static final String[] NOTES={
        "每格只放一个数字。解题依靠已有线索，不需要计算数字的大小或和。",
        "挑战累计填错 3 次结束；不限错练习可以继续尝试。撤销不会返还错误或提示次数。",
        "再点一次选中的数字可以取消。填完的数字仍可在棋盘上点选；擦除或撤销腾出空位后，数字键重新开放。",
        "全盘候选可以一起开关所有空格的候选数。长按游戏数字键，可查看包含该数字的候选格。",
        "选择“先自己想”仍会保留已查看的提示，不重复计次；真正填入后，提示格不能擦除。",
        "暂停、离开对局或切到后台时计时停止。动效可在设置里关闭；学习示例不计入成绩和奖章。"};
    public static final int[] QUICK_CELLS={0,14,24};
    public static Game create(int lesson){
        int[] answer=Game.digits(SOLUTION),puzzle=answer.clone();
        int[] holes=lesson==2?new int[]{0,14,24,41}:lesson==3?new int[]{2,3,11,12,40}:lesson==5?new int[]{2}:new int[]{2,40,70};
        if(lesson!=0)for(int i:holes)puzzle[i]=0;
        if(lesson==3)puzzle=Game.digits("530070000600195000098000060800060003400803001700020006060000280000419005000080079");
        Game game=new Game(puzzle,answer,0);game.practice=true;game.id="tutorial-"+lesson;
        if(lesson==2)game.quickMode=true;
        return game;
    }
    public static int[] region(int kind){return HintEngine.UNITS[kind==0?0:kind==1?9:18].clone();}
    private TutorialLesson(){}
}
