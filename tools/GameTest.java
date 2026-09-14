import com.tankecho.zensudoku.Game;
import java.nio.file.*;
import java.util.*;

public class GameTest {
    static int checks=0;
    static void check(boolean condition,String name){checks++;if(!condition)throw new AssertionError(name);}
    public static void main(String[] args)throws Exception{
        List<String> fixtures=Files.readAllLines(Paths.get(args[0]));
        for(String line:fixtures){String[] a=line.split("\\t");int[] p=Game.digits(a[0]),s=Game.digits(a[1]);
            check(Game.countSolutions(p.clone(),2)==1,"unique original");
            for(int i=0;i<81;i++){check(s[i]>=1&&s[i]<=9,"solution range");check(p[i]==0||p[i]==s[i],"given agrees");for(int j=i+1;j<81;j++)if(Game.peer(i,j))check(s[i]!=s[j],"solution validity");}
            for(int seed=0;seed<10;seed++){int[][] t=Game.transform(p,s,seed);check(Game.countSolutions(t[0].clone(),2)==1,"unique transformed");for(int i=0;i<81;i++)check(t[0][i]==0||t[0][i]==t[1][i],"transform agrees");}
        }
        String[] a=fixtures.get(0).split("\\t");Game g=new Game(Game.digits(a[0]),Game.digits(a[1]),0);
        int empty=-1,second=-1;for(int i=0;i<81;i++)if(g.givens[i]==0){if(empty<0)empty=i;else if(second<0)second=i;}
        g.selected=empty;int expected=Game.mask(g.board,empty);check((expected&(1<<(g.solution[empty]-1)))!=0,"solution in candidates");
        check(g.toggleCandidates()&&g.candidates[empty],"candidate reveal");check(g.toggleCandidates()&&!g.candidates[empty],"candidate hide");
        g.pencil=true;check(g.enter(1)==3&&g.mistakes==0&&g.board[empty]==0,"notes never mistakes");g.pencil=false;
        check(g.enter(g.solution[empty]%9+1)==2&&g.mistakes==1,"wrong move counts");check(g.board[empty]==0,"wrong value not retained");
        check(g.enter(g.solution[empty])==1,"right move");check(g.undo()&&g.board[empty]==0&&g.mistakes==1,"undo doesn't refund errors");
        check(g.hint()&&g.hints==1&&g.hinted[empty],"hint fills selected");check(!g.erase()&&!g.undo(),"hint cannot be erased/undone");
        for(int i=0;i<81&&g.hints<3;i++)if(g.board[i]==0){g.selected=i;check(g.hint(),"hint within budget");}
        for(int i=0;i<81;i++)if(g.board[i]==0){g.selected=i;break;}
        int count=g.filled();check(!g.hint()&&g.hints==3&&g.filled()==count,"fourth hint rejected");
        check(g.enter(g.solution[g.selected]%9+1)==2,"second error");check(g.enter(g.solution[g.selected]%9+1)==5&&g.status.equals("lost"),"third error terminal");
        check(g.enter(g.solution[g.selected])==0&&!g.hint()&&!g.undo(),"terminal game blocked");
        Game win=new Game(Game.digits(a[0]),Game.digits(a[1]),0);for(int i=0;i<81;i++)if(win.board[i]==0){win.selected=i;win.enter(win.solution[i]);}
        check(win.status.equals("won")&&win.filled()==81,"full game victory");for(boolean b:win.completedUnits())check(b,"all row column box complete");
        Game retry=new Game(g.givens,g.solution,g.level);check(retry.mistakes==0&&retry.hints==0&&retry.elapsedMs==0&&retry.status.equals("playing"),"fresh retry budgets");
        Game quick=new Game(g.givens,g.solution,g.level);quick.setQuickMode(true);
        int original=quick.filled(),num=quick.solution[empty];
        check(quick.selectNumber(num)&&quick.activeNumber==num&&quick.highlightNumber()==num,"quick number becomes independent highlight");
        check(quick.filled()==original&&quick.mistakes==0&&quick.hints==0,"choosing quick number never fills or spends budgets");
        check(quick.selectCell(empty)==1&&quick.board[empty]==num,"quick tap fills empty cell");
        for(int i=0;i<81;i++)if(quick.board[i]==0&&quick.solution[i]==num)check(quick.selectCell(i)==1&&quick.activeNumber==(quick.remaining(num)>0?num:0),"quick taps retain number until its ninth occurrence");
        check(!quick.numberAvailable(num)&&quick.activeNumber==0&&!quick.selectNumber(num),"completed quick digit cannot be selected");
        check(quick.completedUnits()[26+num],"nine matching values create digit completion event");
        int completedCell=quick.selected;check(quick.selectCell(completedCell)==0&&quick.highlightNumber()==num,"completed digit still clickable on board");
        check(quick.undo()&&quick.numberAvailable(num),"undo re-enables completed digit");
        check(quick.selectNumber(num),"reopened digit is selectable");
        check(quick.selectCell(completedCell)==1&&quick.activeNumber==0,"refill cancels selection again");
        quick.selectNumber(num%9+1);int errors=quick.mistakes;
        check(quick.selectCell(empty)==0&&quick.board[empty]==num&&quick.mistakes==errors,"quick tap filled editable cell never overwrites");
        int given=0;while(quick.givens[given]==0)given++;
        check(quick.selectCell(given)==0&&quick.mistakes==errors,"quick tap fixed cell never counts error");
        check(quick.selectNumber(num%9+1)&&quick.activeNumber==0,"second number tap deselects");
        int blank=0;while(quick.board[blank]!=0)blank++;
        check(quick.selectCell(blank)==0&&quick.selected==blank&&quick.board[blank]==0,"deselected quick mode permits empty selection");
        check(quick.toggleCandidates()&&quick.hint(),"candidate and hint tools still work after deselection");
        blank=0;while(quick.board[blank]!=0)blank++;quick.pencil=true;int pencilDigit=1;while(!quick.numberAvailable(pencilDigit))pencilDigit++;quick.selectNumber(pencilDigit);
        check(quick.selectCell(blank)==3&&quick.board[blank]==0&&quick.mistakes==errors,"quick pencil records without filling or error");
        check(quick.selectCell(blank)==3&&quick.notes[blank]==0,"quick pencil toggles note");quick.pencil=false;
        quick.setQuickMode(false);check(!quick.quickMode&&quick.activeNumber==0&&quick.selected==-1,"switching mode clears stale number and cell");
        check(quick.selectCell(blank)==0&&quick.board[blank]==0,"classic cell selection never auto fills");
        check(quick.enter(quick.solution[blank])==1,"classic number-after-cell remains usable");
        Game quickLoss=new Game(g.givens,g.solution,g.level);quickLoss.setQuickMode(true);quickLoss.selectNumber(quickLoss.solution[empty]%9+1);
        check(quickLoss.selectCell(empty)==2&&quickLoss.selectCell(empty)==2&&quickLoss.selectCell(empty)==5,"quick mode enforces three-error loss");
        check(!quickLoss.selectNumber(1)&&quickLoss.selectCell(second)==0&&!quickLoss.hint(),"quick mode cannot bypass terminal state");
        System.out.println("PASS: "+checks+" assertions across "+fixtures.size()+" graded puzzles and "+fixtures.size()*10+" transformations; candidates, notes, undo, hint limit, mistake limit, terminal state, win, retry.");
    }
}
