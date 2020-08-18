package de.tuebingen.sfs.iconevol;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Allows to sample from the distribution of mappable strings from one language to another.
 *
 * @author jdellert
 *
 */

public class FormProjectionModel {
    String lang1;
    String lang2;

    Map<String,Double> lang1SegmentCounts;
    Map<String,Double> lang2SegmentCounts;

    double totalSegmentCount;
    private TreeMap<String, Double> gappyBigramCounts;

    Map<String,Map<String,Double>> countForPair;
    Map<String,Map<String,Double>> countForGappyBigram;

    double lang1Stability;

    public FormProjectionModel(String lang1, String lang2) {
        this.lang1 = lang1;
        this.lang2 = lang2;
        this.lang1SegmentCounts = new TreeMap<String,Double>();
        this.lang2SegmentCounts = new TreeMap<String,Double>();
        this.totalSegmentCount = 0;
        this.gappyBigramCounts = new TreeMap<String,Double>();
        this.countForPair = new TreeMap<String,Map<String,Double>>();
        this.countForGappyBigram = new TreeMap<String,Map<String,Double>>();
    }

    public double getLang1Stability() {
        return lang1Stability;
    }

    public void storePair(String lang1Segment, String lang2Segment) {
        storePairWithWeight(lang1Segment, lang2Segment, 1.0);
    }

    public void storePairWithWeight(String lang1Segment, String lang2Segment, double weight) {
        totalSegmentCount += weight;

        Double upperSegmentCount = lang1SegmentCounts.get(lang1Segment);
        if (upperSegmentCount == null) {
            upperSegmentCount = 0.0;
            countForPair.put(lang1Segment, new TreeMap<String,Double>());
        }
        lang1SegmentCounts.put(lang1Segment, upperSegmentCount + weight);

        Double lowerSegmentCount = lang2SegmentCounts.get(lang2Segment);
        if (lowerSegmentCount == null) {
            lowerSegmentCount = 0.0;
        }
        lang2SegmentCounts.put(lang2Segment, lowerSegmentCount + weight);

        Double pairCount = countForPair.get(lang1Segment).get(lang2Segment);
        if (pairCount == null) {
            pairCount = 0.0;
        }
        countForPair.get(lang1Segment).put(lang2Segment, pairCount + weight);
    }

    public void storeGappyBigram(String lang1GappyBigram, String lang2Segment) {
        storeGappyBigramWithWeight(lang1GappyBigram, lang2Segment, 1.0);
    }

    public void storeGappyBigramWithWeight(String lang1GappyBigram, String lang2Segment, double weight) {
        Map<String,Double> gappyBigramCount = countForGappyBigram.get(lang1GappyBigram);
        if (gappyBigramCount == null) {
            gappyBigramCount = new TreeMap<String, Double>();
        }
        countForGappyBigram.put(lang1GappyBigram, gappyBigramCount);

        Double lowerSegmentCount = gappyBigramCount.get(lang2Segment);
        if (lowerSegmentCount == null) {
            lowerSegmentCount = 0.0;
        }
        gappyBigramCount.put(lang2Segment, lowerSegmentCount + weight);
    }

    public void finalizeCounts(int countThreshold) {
        for (String lang1Segment : lang1SegmentCounts.keySet()) {
            double lang1SegmentCount = 0.0;
            for (String lang2Segment : countForPair.get(lang1Segment).keySet()) {
                double count = countForPair.get(lang1Segment).get(lang2Segment);
                if (count < countThreshold) {
                    countForPair.get(lang1Segment).put(lang2Segment, 0.0);
                } else {
                    lang1SegmentCount += count;
                }
            }
            lang1SegmentCounts.put(lang1Segment, lang1SegmentCount);
        }

        for (String gappyBigram : countForGappyBigram.keySet()) {
            double gappyBigramCount = 0.0;
            for (double count : countForGappyBigram.get(gappyBigram).values()) {
                gappyBigramCount += count;
            }
            gappyBigramCounts.put(gappyBigram, gappyBigramCount);
        }

        double numStableSegments = 0.0;
        for (String s1 : countForPair.keySet()) {
            Double stableCount = countForPair.get(s1).get(s1);
            if (stableCount != null)  numStableSegments += stableCount;
        }
        lang1Stability = numStableSegments/totalSegmentCount;
        System.err.println("Overall stability for language pair " + lang1 + " -> " + lang2 + ":\t" + lang1Stability);
    }

    public void recomputeStability(Map<String, String> soundToClass) {
        double numStableSegments = 0.0;
        for (String s1 : countForPair.keySet()) {
            String s1Class = soundToClass.get(s1);
            if (s1Class == null) {
                System.err.println("ERROR: no sound class defined for '" + s1 + "'!");
            }
            for (String s2 : countForPair.get(s1).keySet()) {
                String s2Class = soundToClass.get(s2);
                if (s2Class == null) {
                    System.err.println("ERROR: no sound class defined for '" + s2 + "'!");
                }
                if (!s1Class.equals(s2Class)) continue;
                numStableSegments += countForPair.get(s1).get(s2);
            }
        }
        lang1Stability = numStableSegments/totalSegmentCount;
        System.err.println("Overall stability for language pair " + lang1 + " -> " + lang2 + ":\t" + lang1Stability);
    }

    public List<String> sampleMapping(List<String> lang1String) {
        List<String> sample = new ArrayList<String>(lang1String.size());
        for (int i = 0; i < lang1String.size(); i++) {
            String symbol = lang1String.get(i);
            String gappyBigram = symbol;
            if (i == 0) {
                gappyBigram = "#" + gappyBigram;
            } else {
                gappyBigram = lang1String.get(i-1) + gappyBigram;
            }
            if (i == lang1String.size() - 1) {
                gappyBigram += "#";
            } else {
                gappyBigram += lang1String.get(i+1);
            }

            Double gappyBigramCount = gappyBigramCounts.get(gappyBigram);
            if (gappyBigramCount != null) {
                double cutoff = Math.random() * gappyBigramCount;
                double pos = 0.0;
                for (Entry<String,Double> option : countForGappyBigram.get(gappyBigram).entrySet()) {
                    pos += option.getValue();
                    if (pos >= cutoff) {
                        sample.add(option.getKey());
                        break;
                    }
                }
            } else {
                Double count = lang1SegmentCounts.get(symbol);
                if (count != null) {
                    double cutoff = Math.random() * count;
                    double pos = 0.0;

                    for (Entry<String,Double> option : countForPair.get(symbol).entrySet()) {
                        pos += option.getValue();
                        if (pos >= cutoff) {
                            sample.add(option.getKey());
                            break;
                        }
                    }
                } else {
                    double cutoff = Math.random() * totalSegmentCount;
                    double pos = 0.0;
                    for (Entry<String,Double> option : lang2SegmentCounts.entrySet()) {
                        pos += option.getValue();
                        if (pos >= cutoff) {
                            sample.add(option.getKey());
                            break;
                        }
                    }

                }
            }
        }
        return sample;
    }

}
