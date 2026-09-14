package com.tankecho.zensudoku;

import java.time.LocalDate;

/** Date window and fixed play-time thresholds; unfinished games count too. */
public final class ActivityHeatmap {
    public static final int WEEKS=13;
    public static LocalDate start(LocalDate today){return today.minusDays(today.getDayOfWeek().getValue()-1).minusWeeks(WEEKS-1);}
    public static int level(long ms){return ms<=0?0:ms<600000?1:ms<1800000?2:ms<3600000?3:4;}
    private ActivityHeatmap(){}
}
