import com.tankecho.zensudoku.ActivityHeatmap;
import java.time.*;
public class HeatmapTest {
    static int checks;static void check(boolean value){checks++;if(!value)throw new AssertionError("heatmap check "+checks);}
    public static void main(String[] args){
        long[] ms={-1,0,1,599999,600000,1799999,1800000,3599999,3600000,Long.MAX_VALUE};int[] expected={0,0,1,1,2,2,3,3,4,4};
        for(int i=0;i<ms.length;i++)check(ActivityHeatmap.level(ms[i])==expected[i]);
        for(LocalDate date=LocalDate.of(2024,1,1);date.isBefore(LocalDate.of(2027,1,1));date=date.plusDays(1)){
            LocalDate start=ActivityHeatmap.start(date);check(start.getDayOfWeek()==DayOfWeek.MONDAY);check(!date.isBefore(start)&&!date.isAfter(start.plusDays(90)));check(start.plusDays(90).getDayOfWeek()==DayOfWeek.SUNDAY);check(start.plusDays(84).equals(date.minusDays(date.getDayOfWeek().getValue()-1)));
        }
        System.out.println("PASS "+checks+" heatmap threshold/date-window checks (leap day and year boundaries included)");
    }
}
