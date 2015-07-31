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

        // build sequence database
        ArrayList<ArrayList<ArrayList<Integer>>> sequenceDatabase =
                build_sequential_database_from_file(filepath);
        threshold = (int) Math.floor(minsup * sequenceDatabase.size());
        prefix_span(null, 0, sequenceDatabase);
    }

    public PrefixSpan() {
        this("/home/mhwong/Desktop/prefix_span_dataset/test.ascii", 0.5);
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
                // if yes, we should treat the element after the last elemenet from the pointer
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

        ArrayList<ArrayList<ArrayList<Integer>>> projectedDatabase = new ArrayList<>();
        TreeSet<Integer> projectedFrequentItemList = new TreeSet<>();
        for(int frequentItem: frequency.keySet()) {
            // append it to alpha to form alpha', output
            ArrayList<ArrayList<Integer>> newAlpha = new ArrayList<>();
            if(alpha != null && !alpha.isEmpty()){
                for(ArrayList<Integer> itemset: alpha) {
                    ArrayList<Integer> newItemset = new ArrayList<>(itemset);
                    newAlpha.add(newItemset);
                }
            }
            if(frequentItem < 0) { // negative means it is in the last element of sequence, eg <(ab)>
                if(!newAlpha.get(newAlpha.size()-1).contains(Math.negateExact(frequentItem)));
                    newAlpha.get(newAlpha.size()-1).add(Math.negateExact(frequentItem));
            }
            else { // eg <ab>
                ArrayList<Integer> itemToBeAdd = new ArrayList<>();
                itemToBeAdd.add(frequentItem);
                newAlpha.add(itemToBeAdd);
            }

            // print new alpha
            System.out.print("<");
            for(ArrayList<Integer> itemset: newAlpha){
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
            // build the projected database from this frequent item
            for(ArrayList<ArrayList<Integer>> sequence: sequentialDatabase) {
                // copy a projected sequence
                ArrayList<ArrayList<Integer>> projectedSequence = new ArrayList<>();
                for(ArrayList<Integer> itemset: sequence) {
                    ArrayList<Integer> projectedItemset = new ArrayList<>();
                    for(int item: itemset) {
                        if(frequency.containsKey(item)) {
                            projectedItemset.add(item);
                        }
                    }
                    projectedItemset.sort((o1, o2) -> {
                        if(Integer.compare(Math.abs(o1.intValue()), Math.abs(o2.intValue())) == 0) { //equal
                            return o1.compareTo(o2);
                        }
                        else {
                            return Integer.compare(Math.abs(o1.intValue()), Math.abs(o2.intValue()));
                        }
                    });
                    projectedSequence.add(projectedItemset);
                }

                // TODO: do delete again!

                // delete those doesn't match
                ArrayList<Integer> itemToBeMatch1 = new ArrayList<>();
                ArrayList<Integer> itemToBeMatch2 = new ArrayList<>();
                if(frequentItem < 0) {
                    itemToBeMatch1.addAll(newAlpha.get(newAlpha.size()-1));
                }

                itemToBeMatch2.add(frequentItem);
                Iterator<ArrayList<Integer>> projectSequenceIterator = projectedSequence.iterator();
                while(projectSequenceIterator.hasNext()) {
                    // remove the itemset either it doesn't contain itemToBeMatch1 and itemToBeMatch2
                    ArrayList<Integer> itemset = projectSequenceIterator.next();

                    if(frequentItem < 0) {
                        if(!itemset.containsAll(itemToBeMatch1) && !itemset.containsAll(itemToBeMatch2)) {
                            projectSequenceIterator.remove();
                        }
                        else {
                            // found an itemset that contains all item from the frequent item
                            // we remove the items that are same
                            itemset.removeAll(itemToBeMatch1);
                            itemset.removeAll(itemToBeMatch2);
                            // remove the items that placed before the frequent items
                            if(!itemset.isEmpty() && (!itemToBeMatch1.isEmpty() || !itemToBeMatch2.isEmpty())){
                                Iterator<Integer> itemsetIterator = itemset.iterator();
                                while(itemsetIterator.hasNext()) {
                                    int item = itemsetIterator.next();
                                    int intVal1 = Integer.MIN_VALUE;
                                    int intVal2 = Integer.MIN_VALUE;

                                    if(!itemToBeMatch1.isEmpty()) {
                                        intVal1 = Math.abs(itemToBeMatch1.get(itemToBeMatch1.size()-1));
                                    }

                                    if(!itemToBeMatch2.isEmpty()) {
                                        intVal2 = Math.abs(itemToBeMatch2.get(itemToBeMatch2.size()-1));
                                    }

                                    if(item < intVal1 || item < intVal2) {
                                        itemsetIterator.remove();
                                    }
                                }
                            }
                            // check if itemset left some element
                            if(!itemset.isEmpty()) {
                                // negate the first item -> imply it is in the same itemset with the next prefix
                                itemset.set(0, Math.negateExact(itemset.get(0)));
                            }
                            else {
                                // we remove the itemset if it is empty
                                projectSequenceIterator.remove();
                            }
                            break;
                        }
                    }
                    else {
                        if(!itemset.containsAll(itemToBeMatch2)) {
                            projectSequenceIterator.remove();
                        }
                        else {
                            // found an itemset that contains all item from the frequent item
                            // we remove the items that are same
                            itemset.removeAll(itemToBeMatch2);
                            // remove the items that placed before the frequent items
                            if(!itemset.isEmpty() && !itemToBeMatch2.isEmpty()){
                                Iterator<Integer> itemsetIterator = itemset.iterator();
                                while(itemsetIterator.hasNext()) {
                                    int item = itemsetIterator.next();
                                    int intVal2 = Integer.MIN_VALUE;

                                    if(!itemToBeMatch2.isEmpty()) {
                                        intVal2 = Math.abs(itemToBeMatch2.get(itemToBeMatch2.size()-1));
                                    }

                                    if(item < intVal2) {
                                        itemsetIterator.remove();
                                    }
                                }
                            }
                            // check if itemset left some element
                            if(!itemset.isEmpty()) {
                                // negate the first item -> imply it is in the same itemset with the next prefix
                                itemset.set(0, Math.negateExact(itemset.get(0)));
                            }
                            else {
                                // we remove the itemset if it is empty
                                projectSequenceIterator.remove();
                            }
                            break;
                        }
                    }
                }


                // get the projected item list, will remove those infrequent in next recursive call
                for(ArrayList<Integer> itemset: projectedSequence) {
                    for(int item: itemset) {
                        projectedFrequentItemList.add(item);
                    }
                }

                // add projected sequence into projected database
                if(!projectedSequence.isEmpty()){
                    projectedDatabase.add(projectedSequence);
                }

            }

            // recursive call prefix span
            if(!projectedDatabase.isEmpty()){
                prefix_span(newAlpha, length+1, projectedDatabase);
            }

        }

    }
}
