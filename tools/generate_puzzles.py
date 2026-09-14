"""Generate original unique puzzles, graded by the techniques needed to solve them."""
import random, json, time
from pathlib import Path

R = random.Random(21520260909)
UNITS = [[r*9+c for c in range(9)] for r in range(9)] + [[r*9+c for r in range(9)] for c in range(9)] + [[(br*3+r)*9+bc*3+c for r in range(3) for c in range(3)] for br in range(3) for bc in range(3)]
PEERS = [set().union(*(set(u) for u in UNITS if i in u))-{i} for i in range(81)]
FULL = 511
def bc(n): return bin(n).count('1')
def options(b,i):
    m = 0
    for p in PEERS[i]:
        if b[p]: m |= 1 << (b[p]-1)
    return FULL & ~m

def count(b, limit=2):
    best,mask,n=-1,0,10
    for i,v in enumerate(b):
        if not v:
            m=options(b,i); k=bc(m)
            if not k: return 0
            if k<n: best,mask,n=i,m,k
            if k==1: break
    if best<0: return 1
    total=0
    while mask and total<limit:
        bit=mask&-mask; mask-=bit; b[best]=bit.bit_length()
        total+=count(b,limit-total)
    b[best]=0
    return total

def logic(board, level):
    b=board[:]; c=[options(b,i) if not b[i] else 0 for i in range(81)]
    while 0 in b:
        progress=False
        for i in range(81):
            if not b[i] and bc(c[i])==1:
                bit=c[i]; b[i]=bit.bit_length(); c[i]=0
                for p in PEERS[i]: c[p] &= ~bit
                progress=True
        if progress: continue
        if level>=1:
            for u in UNITS:
                for v in range(9):
                    positions=[i for i in u if c[i] & (1<<v)]
                    if len(positions)==1:
                        i=positions[0]; bit=1<<v; b[i]=v+1; c[i]=0
                        for p in PEERS[i]: c[p]&=~bit
                        progress=True
            if progress: continue
        if level>=2:
            for u in UNITS:
                # Naked pairs.
                for m in set(c[i] for i in u if bc(c[i])==2):
                    pair=[i for i in u if c[i]==m]
                    if len(pair)==2:
                        for i in u:
                            if i not in pair and c[i]&m: c[i]&=~m; progress=True
                # Locked candidates across intersecting units.
                for v in range(9):
                    bit=1<<v; pos=[i for i in u if c[i]&bit]
                    if len(pos)>=2:
                        for other in UNITS:
                            if other!=u and all(i in other for i in pos):
                                for i in other:
                                    if i not in u and c[i]&bit: c[i]&=~bit; progress=True
            if progress: continue
        return False
    return True

def solution():
    def groups():
        a=R.sample(range(3),3)
        return [g*3+i for g in a for i in R.sample(range(3),3)]
    rows,cols,digits=groups(),groups(),R.sample(range(1,10),9)
    return [digits[(r*3+r//3+c)%9] for r in rows for c in cols]

def main():
    banks=[[],[],[],[]]; seen=set(); t=time.time(); attempts=0
    while min(map(len,banks))<12:
        sol=solution(); b=sol[:]; order=R.sample(range(81),81); attempts+=1
        for i in order:
            old=b[i]; b[i]=0
            if count(b[:])!=1: b[i]=old; continue
            clues=sum(v!=0 for v in b)
            level=None
            if clues==42 and logic(b,0): level=0
            elif 32<=clues<=37 and not logic(b,0) and logic(b,1): level=1
            elif 26<=clues<=32 and not logic(b,1) and logic(b,2): level=2
            elif 24<=clues<=29 and not logic(b,2): level=3
            if level is not None and len(banks[level])<12:
                key=''.join(map(str,b))
                if key not in seen:
                    banks[level].append({'puzzle':key,'solution':''.join(map(str,sol)),'clues':clues})
                    seen.add(key)
                    print('banks',list(map(len,banks)),'attempt',attempts,'seconds',round(time.time()-t,1),flush=True)
    dest=Path(__file__).resolve().parents[1]/'android/app/src/main/assets/puzzles.json'
    dest.write_text(json.dumps(banks,separators=(',',':')))
    print('DONE',dest,flush=True)
if __name__=='__main__': main()
