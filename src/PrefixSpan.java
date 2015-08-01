import java.awt.image.AreaAveragingScaleFilter;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by mhwong on 7/30/15.
 */
public class PrefixSpan {

    private boolean printPattern = false;
    // Count how many frequent sequantial pattern generated
    private int count = 0;
    private double minsup;
    private int threshold;

    public PrefixSpan(String filepath, double minsup) {
        this.minsup = minsup;

        // build sequence database
        ArrayList<ArrayList<ArrayList<Integer>>> sequenceDatabase =
                build_sequential_database_from_file(filepath);
        threshold = (int) Math.floor(minsup * sequenceDatabase.size());
        prefix_span(null, 0, sequenceDatabase);
        System.out.printf("Number of frequent sequential pattern: %d\n", count);
    }

    public PrefixSpan() {
        this("/home/mhwong/Desktop/prefix_span_dataset/C50S10T2.5N10000.ascii", 0.01);
    }

    private ArrayList<ArrayList<ArrayList<Integer>>> build_sequential_database_from_file(String filepath) {
        /**
         *  Input File format:
         *  CustId    TransId    Item
         *  1         1          832
         *  1         1          3618
         *  1         1          4315
         *  ...
         */


        File inputFile = new File(filepath);
        try {
            InputStream inputStream = new FileInputStream(inputFile);
            InputStreamReader inputStreamReader =
                    new InputStreamReader(inputStream, Charset.forName("ascii"));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String inputLine;
            ArrayList<Integer> itemset = new ArrayList<>();
            ArrayList<ArrayList<Integer>> sequence = new ArrayList<>();
            ArrayList<ArrayList<ArrayList<Integer>>> sequenceDatabase= new ArrayList<>();

            int prevCustId = 0,
                prevTransId = 0,
                itemId;

            while((inputLine = bufferedReader.readLine()) != null) {

                String[] splitItem = inputLine.split("[ ]+");

                int custId = Integer.parseInt(splitItem[1]);

                // the second is transaction id
                int transId = Integer.parseInt(splitItem[2]);


                // the third is item id
                itemId = Integer.parseInt(splitItem[3]);

                // if transId != prevTransId means it start a new transaction,
                // should insert previous transaction and clear it
                if(transId != prevTransId) {
                    if(!itemset.isEmpty()) {
                        // should sort itemset before insert
                        itemset.sort((o1, o2) -> {
                            if(Integer.compare(Math.abs(o1.intValue()), Math.abs(o2.intValue())) == 0) { //equal
                                return o1.compareTo(o2);
                            }
                            else {
                                return Integer.compare(Math.abs(o1.intValue()), Math.abs(o2.intValue()));
                            }
                        });
                        sequence.add(itemset);
                        itemset = new ArrayList<>();
                    }
                    prevTransId = transId;
                }

                itemset.add(itemId);

                // if custId != prevCustId means he is a new customer,
                // should insert previous customer id and clear it
                if(custId != prevCustId) {
                    if(!sequence.isEmpty()) {
                        sequenceDatabase.add(sequence);
                        sequence = new ArrayList<>();
                    }
                    prevCustId = custId;
                }
            }

            // add the last sequence to sequence database
            sequence.add(itemset);
            sequenceDatabase.add(sequence);

            return sequenceDatabase;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }

    private void prefix_span(ArrayList<ArrayList<Integer>> alpha,
                             int length,
                             ArrayList<ArrayList<ArrayList<Integer>>> sequentialDatabase) {

        // first we build frequency table from sequential database
        HashMap<Integer, Integer> frequency = buildFrequencyTable(alpha, sequentialDatabase);

        for(int frequentItem: frequency.keySet()) {
            // then we update the alpha from the frequency table
            // two thing can be done, for every b in frequency table:
            // (1) b can be assembled to the last element of alpha (b is a negative value)
            // (2) <b> can be appended to alpha to form a sequential pattern

            ArrayList<ArrayList<Integer>> newAlpha = new ArrayList<>();

            if(frequentItem < 0) {
                for(ArrayList<Integer> itemset: alpha) {
                    ArrayList<Integer> newItemset = new ArrayList<>(itemset);
                    newAlpha.add(newItemset);
                }
                // frequentItem assembled to the last element of alpha
                ArrayList<Integer> newItemset = new ArrayList<>(newAlpha.get(newAlpha.size()-1));
                newItemset.add(Math.negateExact(frequentItem));
                newItemset.sort(Integer::compareTo);
                newAlpha.set(newAlpha.size() - 1, newItemset);
            }
            else {
                // case b > 0
                // check if alpha is null or is empty, if not, make a copy
                if(alpha != null && !alpha.isEmpty()) {
                    for(ArrayList<Integer> itemset: alpha) {
                        ArrayList<Integer> newItemset = new ArrayList<>(itemset);
                        newAlpha.add(newItemset);
                    }
                }
                // append <b> to new alpha
                ArrayList<Integer> newItemset = new ArrayList<>();
                newItemset.add(frequentItem);
                newAlpha.add(newItemset);
            }
            // we print out new alpha
            if(printPattern){
                printAlpha(newAlpha);
            }
            count++;

            // we start construct new alpha's projected database
            // start from every sequence in the database
            ArrayList<ArrayList<ArrayList<Integer>>> projectedDatabase = new ArrayList<>();
            for(ArrayList<ArrayList<Integer>> sequence: sequentialDatabase) {

                // we make a copy of sequence
                ArrayList<ArrayList<Integer>> projectedSequence = makeACopyOfSequenceWithFrequencyCheck(sequence, frequency.keySet());

                // we find a suffix wrt the current prefix (new alpha)
                ArrayList<ArrayList<Integer>> suffix = getSuffix(projectedSequence, newAlpha.get(newAlpha.size()-1));

                // if suffix is not null, we push it into projected database
                if(suffix != null && !suffix.isEmpty()) {
                    projectedDatabase.add(suffix);
                }
            }

            // if projected database isn't null, we recursively call prefix span
            if(!projectedDatabase.isEmpty()) {
                prefix_span(newAlpha, length+1, projectedDatabase);
            }
        }

    }

    private HashMap<Integer, Integer> buildFrequencyTable(ArrayList<ArrayList<Integer>> alpha, ArrayList<ArrayList<ArrayList<Integer>>> sequentialDatabase) {
        // first we count frequencies from sequential database
        HashMap<Integer, Integer> frequency = new HashMap<>();

        // a pointer point to the last itemset in alpha

        ArrayList<Integer> alphaLastItemsetPointer = null;
        if(alpha != null && !alpha.isEmpty()) {
            alphaLastItemsetPointer = new ArrayList<>();
            alphaLastItemsetPointer.addAll(alpha.get(alpha.size()-1));
        }

        for(ArrayList<ArrayList<Integer>> sequence: sequentialDatabase) {
            // use a sequence Item to record how many type of item in a sequence
            HashSet<Integer> sequenceItem = new HashSet<>();
            for(ArrayList<Integer> itemSet: sequence) {
                // see if the itemset contain all element from the alpha last itemset pointer
                // if yes, we should treat the element after the last element from the pointer
                // as value with underscore and without
                // eg: prefix = <a>, sequence = <ac>, we threat c in <ac> as c and _c
                if(alphaLastItemsetPointer != null && !alphaLastItemsetPointer.isEmpty() && itemSet.containsAll(alphaLastItemsetPointer)) {
                    // add item in alpha last itemset pointer as usual
                    for(int item: alphaLastItemsetPointer) {
                        sequenceItem.add(item);
                    }
                    ArrayList<Integer> shadowItemSet = new ArrayList<>(itemSet);
                    shadowItemSet.removeAll(alphaLastItemsetPointer);
                    for(int shadowItem: shadowItemSet) {
                        sequenceItem.add(shadowItem);
                        sequenceItem.add(Math.negateExact(shadowItem));
                    }
                }
                else {
                    // if no, treat like normal itemset
                    for(int item: itemSet) {
                        sequenceItem.add(item);
                    }
                }
            }
            // add to frequency table
            for(int item: sequenceItem) {
                if(frequency.containsKey(item)) {
                    frequency.put(item, frequency.get(item) + 1);
                }
                else {
                    frequency.put(item, 1);
                }
            }

            sequenceItem.clear();
        }

        // we remove those infrequent item
        for(Iterator<Integer> iterator = frequency.keySet().iterator(); iterator.hasNext();) {
            if(frequency.get(iterator.next()) < threshold) {
                iterator.remove();
            }
        }

        return frequency;
    }

    private void printAlpha(ArrayList<ArrayList<Integer>> alpha) {
        System.out.print("<");
        for(ArrayList<Integer> itemset: alpha){
            if(itemset.size() > 1) {
                System.out.print("(");
            }
            for(int item: itemset) {
                System.out.printf("%d ", item);
            }
            if(itemset.size() > 1) {
                System.out.print(")");
            }
        }
        System.out.println(">");
    }

    private ArrayList<ArrayList<Integer>> makeACopyOfSequenceWithFrequencyCheck(ArrayList<ArrayList<Integer>> sequence, Set<Integer> frequency) {

        ArrayList<ArrayList<Integer>> copyOfSequence = new ArrayList<>();
        for(ArrayList<Integer> itemset: sequence) {
            ArrayList<Integer> copyOfItemset = makeACopyOfItemsetWithFrequencyCheck(itemset, frequency);
            if(!copyOfItemset.isEmpty()) {
                copyOfSequence.add(copyOfItemset);
            }
        }
        return copyOfSequence;
    }

    private ArrayList<Integer> makeACopyOfItemsetWithFrequencyCheck(ArrayList<Integer> itemset, Set<Integer> frequency) {
        ArrayList<Integer> copyOfItemset = new ArrayList<>();
        for(int item: itemset) {
            // only add those are frequent
            if(frequency.contains(item)) {
                copyOfItemset.add(item);
            }
        }

        return copyOfItemset;
    }

    private ArrayList<ArrayList<Integer>> makeACopyOfSequence(List<ArrayList<Integer>> sequence) {

        ArrayList<ArrayList<Integer>> copyOfSequence = new ArrayList<>();
        for(ArrayList<Integer> itemset: sequence) {
            ArrayList<Integer> copyOfItemset = makeACopyOfItemset(itemset);
            if(!copyOfItemset.isEmpty()) {
                copyOfSequence.add(copyOfItemset);
            }
        }
        return copyOfSequence;
    }

    private ArrayList<Integer> makeACopyOfItemset(ArrayList<Integer> itemset) {
        ArrayList<Integer> copyOfItemset = new ArrayList<>();
        for(int item: itemset) {
            copyOfItemset.add(item);
        }

        return copyOfItemset;
    }

    private ArrayList<ArrayList<Integer>> getSuffix(ArrayList<ArrayList<Integer>> sequence, ArrayList<Integer> prefix) {

        boolean found = false;
        int i;

        // a pointer to the last item of prefix
        int prefixLastItem = prefix.get(prefix.size()-1);
        for(i = 0; i < sequence.size(); i++) {
            if(sequence.get(i).containsAll(prefix)) {
                found = true;
                break;
            }
            else if(prefix.size() > 1 && sequence.get(i).get(0) < 0 &&prefixLastItem == Math.negateExact(sequence.get(i).get(0))) {
                found = true;
                break;
            }
        }

        if(found) {
            ArrayList<ArrayList<Integer>> suffix = makeACopyOfSequence(sequence.subList(i, sequence.size()));
            ArrayList<Integer> itemset = suffix.get(0);
            if(itemset.get(0) < 0) {
                itemset.remove(0);
            }
            else {
                itemset.removeAll(prefix);
                // remove things before prefix
                Iterator<Integer> iterator = itemset.iterator();
                while(iterator.hasNext()) {
                    int item = iterator.next();
                    if(item < prefixLastItem) {
                        iterator.remove();
                    }
                }
            }
            if(!itemset.isEmpty()) {
                itemset.sort(Integer::compareTo);
                itemset.set(0, Math.negateExact(itemset.get(0)));
                suffix.set(0, itemset);
            }
            else {
                suffix.remove(0);
            }
            return suffix;
        }
        else {
            return null;
        }
    }
}
