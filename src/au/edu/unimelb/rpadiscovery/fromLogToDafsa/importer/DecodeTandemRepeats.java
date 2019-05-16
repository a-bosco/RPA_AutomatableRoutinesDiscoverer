package au.edu.unimelb.rpadiscovery.fromLogToDafsa.importer;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;


public class DecodeTandemRepeats
{
    IntArrayList trace;
    int[] arTrace;
    int[] SA;
    int n;
    int k;
    IntArrayList p = new IntArrayList(); //starting positions of each LZ77 Block
    IntArrayList l = new IntArrayList(); //length of each LZ77 Block
    IntObjectHashMap<IntHashSet> tandemRepeats = new IntObjectHashMap<IntHashSet>();
    IntObjectHashMap<UnifiedSet<Couple<Integer,Integer>>> maximalPrimitiveRepeats = new IntObjectHashMap<>();;
    private IntArrayList reducedTrace;
    private IntIntHashMap reducedLabels;
    private IntArrayList adjustedCost;

    /*public static void main(String[] args)
    {
        IntArrayList Main = new IntArrayList();
        //Main.addAll(1,2,1,1,2,1,1,2,2,1,1,1,2,1,1,2,1);
        //Main.addAll(2,1,2,2,1,2,1,2,2,2,1,2);
        //Main.addAll(1,2,3,3,2,3,3,2,4,4,2,2);
        //Main.addAll(1,2,3,3,3,3,3);
        //Main.addAll(1,2,3,2,3,2,3,2,3,2,3,4,5,6);
        //Main.addAll(1,2,3,2,3,2,3,4,5,3,4,3,4,3,4,5,6);
        Main.addAll(1,2,3,4,5);
        System.out.println(Main);
        DecodeTandemRepeats decoder = new DecodeTandemRepeats(Main,0,Main.size()*2);
        System.out.println("p: " +decoder.p);
        System.out.println("l: " +decoder.l);
        decoder.findMaximalPrimitiveTandemrepeats();
        //for(Couple<Integer,Integer> repeat : decoder.maximalPrimitiveRepeats.keySet())
        //{
        //    System.out.println("Repeat start: " + repeat.getFirstElement() + ", k: " + repeat.getSecondElement() + ", maximal length " + decoder.maximalPrimitiveRepeats.get(repeat));
        //}
        decoder.reduceTandemRepeats();
    }*/

    public DecodeTandemRepeats(IntArrayList trace, int $, int $$)
    {
        this.trace = trace;
        n = k = trace.size();
        arTrace = new int[n+2];
        arTrace[0] = $;
        arTrace[n+1] = $$;
        for(int pos = 0; pos <trace.size(); pos++) arTrace[pos+1] = trace.get(pos);
        SA = new SuffixArray(arTrace).getSA();
        //sais.suffixsort(this.arTrace, this.SA, this.n, n+2); //had some issue
        //KKP3(); //LZ77 with time complexity O(n) and space complexity O(3 n log(n))
        KKP2(); //LZ77 with time complexity O(n) and space complexity O(2 n log(n))
        detectTandemRepeatTypes();
    }

    public IntArrayList reducedTrace()
    {
        if(reducedTrace==null)
            reduceTandemRepeats();
        return reducedTrace;
    }

    public IntIntHashMap reducedLabels()
    {
        if(reducedLabels==null)
            reduceTandemRepeats();
        return reducedLabels;
    }

    public IntArrayList adjustedCost()
    {
        if(adjustedCost==null)
            reduceTandemRepeats();
        return adjustedCost;
    }

    public void reduceTandemRepeats()
    {
       if(!tandemRepeats.isEmpty())
           findMaximalPrimitiveTandemrepeats();
       UnifiedSet<Couple<Integer, Integer>> repeatLengths;
       int maxLength, maxK, curLength;
       IntArrayList startReduce = new IntArrayList();
       IntArrayList reduceLength = new IntArrayList();
       IntArrayList reduceTo = new IntArrayList();
       for(int startPos = 0; startPos < n + 1; startPos++)
       {
           if((repeatLengths = maximalPrimitiveRepeats.get(startPos)) != null)
           if(repeatLengths.size()!=0)
           {
               maxLength = 0;
               maxK = 1;
               for(Couple<Integer, Integer> repeatLength : repeatLengths)
               {
                   curLength = repeatLength.getFirstElement() * repeatLength.getSecondElement();
                   if(curLength > maxLength)
                   {
                       maxLength = curLength;
                       maxK = repeatLength.getFirstElement();
                   }
               }
               for(int pos = startPos + 1; pos < startPos + maxLength; pos++)
               {
                   if((repeatLengths = maximalPrimitiveRepeats.get(pos)) != null)
                   if (repeatLengths.size() != 0) {
                       for (Couple<Integer, Integer> repeatLength : repeatLengths)
                       {
                           curLength = repeatLength.getFirstElement() * repeatLength.getSecondElement();
                           if (curLength > maxLength) {
                               maxLength = curLength;
                               maxK = repeatLength.getFirstElement();
                               startPos = pos;
                               pos = startPos + 1;
                           }
                       }
                   }
               }
               startReduce.add(startPos);
               reduceLength.add(maxLength);
               reduceTo.add(maxK*2);
               startPos += maxLength-1;
           }
       }
       reducedLabels = new IntIntHashMap();
       //System.out.println("Start Pos : " + startReduce);
       //System.out.println("Reduce Length : " + reduceLength);
       //System.out.println("Reduce to : " + reduceTo);

       reducedTrace = new IntArrayList();
       reducedTrace.addAll(arTrace);
       for(int toReduce = startReduce.size()-1; toReduce >= 0; toReduce--)
       {
           for(int pos = 0; pos < reduceLength.get(toReduce) - reduceTo.get(toReduce);pos++)
           {
               reducedLabels.addToValue(reducedTrace.get(startReduce.get(toReduce)),1);
               reducedTrace.removeAtIndex(startReduce.get(toReduce));
           }
       }
       reducedTrace.removeAtIndex(reducedTrace.size()-1);
       reducedTrace.removeAtIndex(0);
       //System.out.println("Reduced Trace : " + reducedTrace);
       //System.out.println("Reduced Labels: " + reducedLabels);
       adjustedCost = new IntArrayList();
       int posToReduce=0;
       int cost, copy;
       for(int pos = 0; pos < reducedTrace.size(); pos++)
       {
           if(posToReduce<startReduce.size()) {
               if (pos == startReduce.get(posToReduce) - 1) {
                    for(int pos2=posToReduce+1; pos2<startReduce.size();pos2++)
                    {
                       copy = startReduce.removeAtIndex(pos2);
                       startReduce.addAtIndex(pos2, copy - (reduceLength.get(posToReduce) - reduceTo.get(posToReduce)));
                    }
                    if(reduceLength.get(posToReduce) > reduceTo.get(posToReduce)) {
                        cost = (reduceLength.get(posToReduce) - reduceTo.get(posToReduce) / 2) / (reduceTo.get(posToReduce) / 2);
                    }
                    else cost = 1;
                   for(int pos2 = pos; pos2 < pos + reduceTo.get(posToReduce);pos2++)
                       adjustedCost.add(cost);

                    pos+=reduceTo.get(posToReduce)-1;
                    posToReduce++;
               } else
                   adjustedCost.add(1);
           }
           else
               adjustedCost.add(1);
       }
       adjustedCost.add(1);
       //System.out.println(trace + " => " + (trace.size() - reducedTrace.size()));
       //System.out.println(adjustedCost);
    }

    public void findMaximalPrimitiveTandemrepeats()
    {
        IntHashSet alphabet, posOneKs;
        IntArrayList alpha;
        //UnifiedMap<IntHashSet, UnifiedSet<IntArrayList>> alphabetRepeatMapping = new UnifiedMap<IntHashSet, UnifiedSet<IntArrayList>>();
        //UnifiedSet<IntArrayList> primitiveTandemRepeats;
        //IntObjectHashMap<UnifiedSet<IntArrayList>> posPrimitiveRepeatsMapping = new IntObjectHashMap<>();
        for(int pos : tandemRepeats.keySet().toArray())
        {
            for(int k : tandemRepeats.get(pos).toArray())
            {
                alpha = new IntArrayList();
                alphabet = new IntHashSet();
                for(int i = pos; i < pos + k; i++)
                {
                    alpha.add(arTrace[i]);
                    alphabet.add(arTrace[i]);
                }
                if(alphabet.size() == 1 || alphabet.size() == alpha.size() )
                {
                    if(alpha.size() > alphabet.size()) continue;
                    //tandem repeat is primitive
                    determineMaxRepetitions(pos, alpha);
                }
                else if(alpha.size() % 2 == 0)
                {
                    DecodeTandemRepeats decoder = new DecodeTandemRepeats(alpha,arTrace[0],arTrace[n+1]);
                    if((posOneKs = decoder.tandemRepeats.get(1)) != null)
                    {
                        if((posOneKs.contains(alpha.size() / 2)))//alpha is a primitive repeat type
                            determineMaxRepetitions(pos, alpha);
                    }
                }
                else
                {
                    determineMaxRepetitions(pos, alpha);
                }
            }
        }

    }

    private void determineMaxRepetitions(int start, IntArrayList primitiveRepeat)
    {
        int nReps = 2;
        int startNewIt, endNewIt, length = primitiveRepeat.size();
        UnifiedSet<Couple<Integer, Integer>> repeatLengths;
        boolean valid = true;
        while(valid) {
            startNewIt = start + length * nReps;
            endNewIt = startNewIt + length - 1;
            if(endNewIt > n) break;
            for (int pos = startNewIt; pos<=endNewIt; pos++)
                if(arTrace[pos] != primitiveRepeat.get(pos-startNewIt))
                {
                    valid = false;
                    break;
                }
            if(valid) nReps++;
        }
        Couple<Integer,Integer> repeatLength = new Couple<>(length, nReps);
        if((repeatLengths = maximalPrimitiveRepeats.get(start))==null)
        {
            repeatLengths = new UnifiedSet<Couple<Integer,Integer>>();
            maximalPrimitiveRepeats.put(start, repeatLengths);
        }
        repeatLengths.add(repeatLength);

    }

    private void detectTandemRepeatTypes()
    {
        for(int block = 0; block < p.size() - 1; block++)
        {
            Algorithm1A(p.get(block + 1), l.get(block));
            Algorithm1B(p.get(block),l.get(block),p.get(block+1),l.get(block+1));
        }
    }

    private void Algorithm1A(int h1, int lengthB)
    {
        int q, k1, k2, start;
        IntHashSet ks;
        for(int k = 1; k <= lengthB; k++)
        {
            q = h1 - k;
            k1 = 0;
            while((h1 + k1 + 1 < n+2) && (arTrace[h1 + k1] == arTrace[q + k1])) k1++;
            k2 = 0;
            while((q - k2 - 1 > 0) && (arTrace[h1 - k2 - 1] == arTrace[q - k2 - 1])) k2++;
            start = Math.max(q - k2, q - k);
            if(k1 + k2 >=k && k1 > 0)
            {
                //System.out.println("A: " + start + " " + k);
                if((ks = tandemRepeats.get(start))==null)
                {
                    ks = new IntHashSet();
                    tandemRepeats.put(start,ks);
                }
                ks.add(k);
            }
        }
    }

    private void Algorithm1B(int h, int lengthB, int h1, int lengthB1)
    {
        int q, k1, k2, start;
        IntHashSet ks;
        for(int k = 1; k <= lengthB + lengthB1; k++)
        {
            q = h + k;
            k1 = 0;
            while((q + k1 + 1 < n +2) && (arTrace[q+k1]==arTrace[h+k1])) k1++;
            k2=0;
            while((h - k2 -1 > 0) && (arTrace[h-k2 - 1] == arTrace[q - k2 -1])) k2++;
            start = Math.max(h-k2,h-k);
            if(k1+k2 >= k && k1 > 0 && (start + k - 1 < h1) && k2 > 0)
            {
                //System.out.println("B: " + start + " " + k);
                if((ks = tandemRepeats.get(start))==null)
                {
                    ks = new IntHashSet();
                    tandemRepeats.put(start,ks);
                }
                ks.add(k);
            }
        }
    }

    private void KKP3()
    {
        int[] copy = new int[n+2];
        copy[0] = copy[n+1] = 0;
        for(int i=1; i < n + 1;i++) copy[i] = SA[i-1];
        SA = copy;
        int i,top = 0;
        int[] psv = new int[n+2], nsv = new int[n+2];
        for(i = 1; i <=n+1; i++)
        {
            while(SA[top] > SA[i])
            {
                nsv[SA[top]] = SA[i];
                psv[SA[top]] = SA[top-1];
                top--;
            }
            top++;
            SA[top] = SA[i];
        }
        i = 1;
        while(i <= n)
            i = lzFactor(i,psv[i],nsv[i]);
    }

    private void KKP2()
    {
        //int[] copy = new int[n+2];
        //copy[0] = copy[n+1] = 0;
        //for(int i=1; i < n + 1;i++) copy[i] = SA[i-1];
        //SA = copy;
        SA[0] = SA[n+1] = 0;
        int top = 0;
        int[] phi = new int[n+2];
        for(int i = 1; i<=n+1;i++) {
            while (SA[top] > SA[i]) {
                phi[SA[top]] = SA[i];
                top--;
            }
            top++;
            SA[top] = SA[i];
        }
        phi[0] = 0;
        int psv,nsv,next = 1;

        for(int t = 1; t <= n; t++)
        {
            nsv = phi[t];
            psv = phi[nsv];
            if(t==next)
                next = lzFactor(t, psv, nsv);
            phi[t] = psv;
            phi[nsv] = t;
        }
        p.add(n+1);
        l.add(0);
    }

    private int lzFactor(int i, int psv, int nsv)
    {
        int l_psv = lcp(i,psv);
        int l_nsv = lcp(i,nsv);

        if(l_psv > l_nsv)
            {p.add(i); l.add(l_psv);}
        else
            {p.add(i); l.add(Math.max(1,l_nsv));}

        return i + Math.max(1,l.get(p.indexOf(i)));
    }

    private int lcp(int i, int pos)
    {
        int l, max;
        l=0;
        for(l = 0 ; l <= n-i; l++)
        {
            if(arTrace[i+l] != arTrace[pos+l])
                break;
        }
        return l;
    }




}
