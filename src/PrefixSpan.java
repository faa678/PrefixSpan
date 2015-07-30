import java.awt.image.AreaAveragingScaleFilter;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by mhwong on 7/30/15.
 */
public class PrefixSpan {

    // Count how many frequent sequantial pattern generated
    private int count = 0;
    private double minsup;
    private int threshold;

    public PrefixSpan(String filepath, double minsup) {
        this.minsup = minsup;

        // a frequent itemset, will remove those infrequent in prefix span
        TreeSet<Integer> frequentItemset = new TreeSet<>();

        // build sequence database
        ArrayList<ArrayList<ArrayList<Integer>>> sequenceDatabase =
                build_sequential_database_from_file(filepath, frequentItemset);
        threshold = (int) Math.floor(minsup * sequenceDatabase.size());
        prefix_span(null, 0, sequenceDatabase, frequentItemset);
    }

    public PrefixSpan() {
        this("/home/mhwong/Desktop/prefix_span_dataset/test.ascii", 0.75);
    }

    private ArrayList<ArrayList<ArrayList<Integer>>> build_sequential_database_from_file(
            String filepath, TreeSet<Integer> frequentItemset) {
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
                        itemset.sort((o1, o2) -> o1.compareTo(o2));
                        sequence.add(itemset);
                        itemset = new ArrayList<>();
                    }
                    prevTransId = transId;
                }

                itemset.add(itemId);
                // add all kinds of item first, remove those infrequent in prefix span
                frequentItemset.add(itemId);

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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }

    private void prefix_span(ArrayList<ArrayList<Integer>> alpha,
                             int length,
                             ArrayList<ArrayList<ArrayList<Integer>>> sequentialDatabase,
                             TreeSet<Integer> frequentItemset) {

        // first we count frequencies from sequential database
        HashMap<Integer, Integer> frequency = new HashMap<>();
        for(int item: frequentItemset) {
            next_sequence: for(ArrayList<ArrayList<Integer>> sequence: sequentialDatabase) {
                for(ArrayList<Integer> itemSet: sequence) {
                    if(itemSet.contains(item)) {
                        if(frequency.containsKey(item)) {
                            frequency.put(item, frequency.get(item) + 1);
                        }
                        else {
                            frequency.put(item, 1);
                        }
                        continue next_sequence;
                    }
                }
            }
        }

        // we remove those infrequent item
        for(Iterator<Integer> iterator = frequency.keySet().iterator(); iterator.hasNext();) {
            if(frequency.get(iterator.next()) < threshold) {
                iterator.remove();
            }
        }
        frequentItemset.retainAll(frequency.keySet());

        ArrayList<ArrayList<ArrayList<Integer>>> projectedDatabase = new ArrayList<>();
        TreeSet<Integer> projectedFrequentItemList = new TreeSet<>();
        for(int frequentItem: frequentItemset) {
            // append it to alpha to form alpha', output
            ArrayList<ArrayList<Integer>> newAlpha = new ArrayList<>();
            for(ArrayList<Integer> itemset: alpha) {
                ArrayList<Integer> newItemset = new ArrayList<>(itemset);
                newAlpha.add(newItemset);
            }
            if(frequentItem < 0) { // negative means it is in the last element of sequence, eg <(ab)>
                newAlpha.get(newAlpha.size()-1).add(Math.negateExact(frequentItem));
            }
            else { // eg <ab>
                newAlpha.add(new ArrayList<>(frequentItem));
            }

            // build the projected database from this frequent item
            for(ArrayList<ArrayList<Integer>> sequence: sequentialDatabase) {
                // copy a projected sequence
                ArrayList<ArrayList<Integer>> projectedSequence = new ArrayList<>();
                for(ArrayList<Integer> itemset: sequence) {
                    ArrayList<Integer> projectedItemset = new ArrayList<>(itemset);
                    projectedSequence.add(projectedItemset);
                }

                // TODO: do delete again!

                // delete those doesn't match
                ArrayList<Integer> itemToBeMatch = new ArrayList<>();
                itemToBeMatch.addAll(alpha.get(alpha.size()-1));
                for(ArrayList<Integer> itemset: projectedSequence) {
                    if(!itemset.containsAll(itemToBeMatch)) {
                        projectedSequence.remove(itemset);
                    }
                    else {
                        break;
                    }
                }

                // get the projected item list, will remove those infrequent in next recursive call
                for(ArrayList<Integer> itemset: projectedSequence) {
                    for(int item: itemset) {
                        projectedFrequentItemList.add(item);
                    }
                }

                // add projected sequence into projected database
                projectedDatabase.add(projectedSequence);
            }

            // recursive call prefix span
            if(!projectedDatabase.isEmpty()){
                prefix_span(newAlpha, length+1, projectedDatabase, projectedFrequentItemList);
            }

        }

    }
}
