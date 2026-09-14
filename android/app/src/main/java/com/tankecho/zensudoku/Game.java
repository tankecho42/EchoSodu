package com.tankecho.zensudoku;

import java.util.*;

/** Android-independent rules. Errors and hint budgets can never be undone. */
public final class Game {
    public static final String[] LEVELS={"简单","中等","困难","专家"};
    public int[] givens, solution, board, notes=new int[81];
    public boolean[] candidates=new boolean[81], hinted=new boolean[81];
    public int selected=-1, mistakes=0, hints=0, level;
    public long elapsedMs=0;
    public String id=UUID.randomUUID().toString(), dailyDate="", status="playing";
    public boolean pencil=false,quickMode=false,practice=false,allCandidates=false;
    public String puzzleId="",technique="";
    public int inspectNumber=0,pendingHintCell=-1,pendingHintDigit=0;
    public String pendingHintReason="";public int[] pendingHintRelated=new int[0];
    public int activeNumber=0;
    private final Deque<int[][]> undo=new ArrayDeque<>();

    public Game(int[] puzzle,int[] answer,int difficulty) {
        givens=puzzle.clone(); solution=answer.clone(); board=puzzle.clone(); level=difficulty;
    }
    public static int[] digits(String s) {
        if(s.length()!=81) throw new IllegalArgumentException("81 digits required");
        int[] a=new int[81]; for(int i=0;i<81;i++) a[i]=s.charAt(i)-'0'; return a;
    }
    public static String encode(int[] a) { StringBuilder s=new StringBuilder(); for(int v:a)s.append(v);return s.toString(); }
    public static boolean peer(int i,int j) {
        return i/9==j/9 || i%9==j%9 || (i/27==j/27 && (i%9)/3==(j%9)/3);
    }
    public static int mask(int[] b,int i) {
        if(b[i]!=0) return 0;
        int mask=511; for(int j=0;j<81;j++) if(peer(i,j) && b[j]>0) mask&=~(1<<(b[j]-1));
        return mask;
    }
    public int filled() { int n=0;for(int v:board)if(v!=0)n++;return n; }
    public int remaining(int n) {int k=9;for(int v:board)if(v==n)k--;return k;}
    public boolean numberAvailable(int n){return n>=1&&n<=9&&remaining(n)>0;}
    public void normalizeActiveNumber(){if(activeNumber>0&&!numberAvailable(activeNumber))activeNumber=0;}
    public void setQuickMode(boolean enabled){quickMode=enabled;activeNumber=0;selected=-1;}
    /** Choosing a number in quick mode never changes the board or spends a budget. */
    public boolean selectNumber(int number){
        if(!quickMode||!status.equals("playing")||!numberAvailable(number))return false;
        inspectNumber=0;activeNumber=activeNumber==number?0:number;selected=-1;return true;
    }
    public int highlightNumber(){if(inspectNumber>0)return inspectNumber;return quickMode&&activeNumber>0?activeNumber:selected>=0?board[selected]:0;}
    /** Tapping any occupied cell only selects it, including player-entered cells. */
    public int selectCell(int index){
        if(!status.equals("playing")||index<0||index>=81)return 0;
        selected=index;
        return inspectNumber==0&&quickMode&&activeNumber>0&&board[index]==0?enter(activeNumber):0;
    }
    public boolean editable() {return status.equals("playing")&&selected>=0&&selected<81&&givens[selected]==0&&!hinted[selected];}
    private void checkpoint(){int[] c=new int[81];for(int i=0;i<81;i++)c[i]=candidates[i]?1:0;undo.push(new int[][]{board.clone(),notes.clone(),c});if(undo.size()>100)undo.removeLast();}
    public boolean canUndo(){return !undo.isEmpty()&&status.equals("playing");}
    public boolean undo(){if(!canUndo())return false;int[][] a=undo.pop();board=a[0];notes=a[1];if(a.length>2)for(int i=0;i<81;i++)candidates[i]=a[2][i]!=0;normalizeActiveNumber();clearPendingHint();return true;}
    /** 0=no-op, 1=correct, 2=wrong, 3=note, 4=won, 5=lost. */
    public int enter(int number) {
        if(!editable()||number<1||number>9)return 0;
        if(pencil) {
            if(board[selected]!=0)return 0;
            checkpoint();notes[selected]^=1<<(number-1);return 3;
        }
        if(board[selected]==number)return 0;
        if(number!=solution[selected]){mistakes++;if(!practice&&mistakes>=3){status="lost";return 5;}return 2;}
        checkpoint();put(number);
        if(filled()==81){status="won";return 4;}return 1;
    }
    private void put(int number){clearPendingHint();board[selected]=number;notes[selected]=0;candidates[selected]=false;for(int j=0;j<81;j++)if(peer(selected,j))notes[j]&=~(1<<(number-1));normalizeActiveNumber();}
    public boolean erase(){if(!editable()||(board[selected]==0&&notes[selected]==0))return false;checkpoint();clearPendingHint();board[selected]=0;notes[selected]=0;return true;}
    public boolean toggleCandidates(){if(!status.equals("playing")||selected<0||board[selected]!=0)return false;if(allCandidates){for(int i=0;i<81;i++)candidates[i]=board[i]==0;allCandidates=false;}candidates[selected]=!candidates[selected];return true;}
    public boolean hint(){
        if(!status.equals("playing")||hints>=3||selected<0||board[selected]!=0)return false;
        hints++;undo.clear();put(solution[selected]);hinted[selected]=true;
        if(filled()==81)status="won";return true;
    }
    public boolean showCandidates(int i){return board[i]==0&&(allCandidates||candidates[i]);}
    public void toggleAllCandidates(){if(!status.equals("playing"))return;allCandidates=!allCandidates;if(!allCandidates)Arrays.fill(candidates,false);}
    public void inspect(int n){inspectNumber=inspectNumber==n?0:n;activeNumber=0;}
    public List<int[][]> undoSnapshots(){return new ArrayList<>(undo);}
    public void restoreUndo(List<int[][]> entries){undo.clear();for(int[][] entry:entries){if(undo.size()==100)break;undo.addLast(entry);}}
    public void clearPendingHint(){pendingHintCell=-1;pendingHintDigit=0;pendingHintReason="";pendingHintRelated=new int[0];}
    public boolean reserveHint(HintEngine.Plan plan){if(!status.equals("playing")||hints>=3||plan==null||board[plan.cell]!=0||solution[plan.cell]!=plan.digit)return false;hints++;pendingHintCell=plan.cell;pendingHintDigit=plan.digit;pendingHintReason=plan.explanation;pendingHintRelated=plan.related.clone();return true;}
    public boolean applyPendingHint(){if(!status.equals("playing")||pendingHintCell<0||board[pendingHintCell]!=0)return false;selected=pendingHintCell;int number=pendingHintDigit;undo.clear();put(number);hinted[selected]=true;if(filled()==81)status="won";return true;}
    public boolean[] completedUnits(){
        boolean[] done=new boolean[36]; Arrays.fill(done,true);
        for(int i=0;i<81;i++)if(board[i]==0){done[i/9]=false;done[9+i%9]=false;done[18+i/27*3+(i%9)/3]=false;}
        for(int n=1;n<=9;n++)done[26+n]=remaining(n)==0;
        return done;
    }
    public static int countSolutions(int[] board,int limit){
        int at=-1,opts=0,best=10;
        for(int i=0;i<81;i++)if(board[i]==0){int m=mask(board,i),c=Integer.bitCount(m);if(c==0)return 0;if(c<best){at=i;opts=m;best=c;}if(c==1)break;}
        if(at<0)return 1;
        int total=0;while(opts!=0&&total<limit){int bit=opts&-opts;opts-=bit;board[at]=Integer.numberOfTrailingZeros(bit)+1;total+=countSolutions(board,limit-total);}board[at]=0;return total;
    }
    public static int[][] transform(int[] puzzle,int[] answer,long seed){
        Random rng=new Random(seed);int[] rows=axis(rng),cols=axis(rng),nums={1,2,3,4,5,6,7,8,9};shuffle(nums,rng);boolean transpose=rng.nextBoolean();
        int[][] result=new int[2][81];
        for(int r=0;r<9;r++)for(int c=0;c<9;c++){int src=transpose?cols[c]*9+rows[r]:rows[r]*9+cols[c];result[0][r*9+c]=puzzle[src]==0?0:nums[puzzle[src]-1];result[1][r*9+c]=nums[answer[src]-1];}
        return result;
    }
    private static int[] axis(Random r){int[] groups={0,1,2};shuffle(groups,r);int[] a=new int[9];for(int g=0;g<3;g++){int[] inner={0,1,2};shuffle(inner,r);for(int k=0;k<3;k++)a[g*3+k]=groups[g]*3+inner[k];}return a;}
    private static void shuffle(int[] a,Random r){for(int i=a.length-1;i>0;i--){int j=r.nextInt(i+1),t=a[i];a[i]=a[j];a[j]=t;}}
}
