package joshua.zmert;
import java.math.*;
import java.util.*;
import java.io.*;

public class TERMinusBLEU extends EvaluationMetric
{
  // individual components
  private TER myTER;
  private BLEU myBLEU;
  private int suffStatsCount_TER;
  private int suffStatsCount_BLEU;

  public TERMinusBLEU(String[] Metric_options)
  {
    // M_o[0]: case sensitivity, case/nocase
    // M_o[1]: with-punctuation, punc/nopunc
    // M_o[2]: beam width, positive integer
    // M_o[3]: maximum shift distance, positive integer
    // M_o[4]: filename of tercom jar file
    // M_o[5]: maximum gram length, positive integer
    // M_o[6]: effective length calculation method, closest/shortest/average

    // for 0-3, default values in tercom-0.7.25 are: nocase, punc, 20, 50

    myTER = new TER(Metric_options);
    myBLEU = new BLEU(Integer.parseInt(Metric_options[5]),Metric_options[6]);

    initialize(); // set the data members of the metric
  }

  protected void initialize()
  {
    metricName = "TER-BLEU";
    toBeMinimized = true;
    suffStatsCount_TER = myTER.get_suffStatsCount();
    suffStatsCount_BLEU = myBLEU.get_suffStatsCount();
    suffStatsCount = suffStatsCount_TER + suffStatsCount_BLEU;
  }

  public double bestPossibleScore() { return -1.0; }
  public double worstPossibleScore() { return (+1.0 / 0.0); }

  public int[] suffStats(String cand_str, int i)
  {
    // this method should never be used when the metric is TER-BLEU,
    // because TERMinusBLEU.java overrides suffStats(String[],int[]) below,
    // which is the only method that calls suffStats(Sting,int).
    return null;
  }

  public int[][] suffStats(String[] cand_strings, int[] cand_indices)
  {
    // calculate sufficient statistics for each sentence in an arbitrary set of candidates

    int candCount = cand_strings.length;
    if (cand_indices.length != candCount) {
      System.out.println("Array lengths mismatch in suffStats(String[],int[]); returning null.");
      return null;
    }

    int[][] stats = new int[candCount][suffStatsCount];
      // size candCount x suffStatsCount
      //    = candCount x (suffStatsCount_TER + suffStatsCount_BLEU)

    int[][] stats_TER = myTER.suffStats(cand_strings,cand_indices);
      // size candCount x suffStatsCount_TER
    int[][] stats_BLEU = myBLEU.suffStats(cand_strings,cand_indices);
      // size candCount x suffStatsCount_BLEU

    for (int d = 0; d < candCount; ++d) {
      int s = 0;
      for (int s_T = 0; s_T < suffStatsCount_TER; ++s_T) {
        stats[d][s] = stats_TER[d][s_T];
        ++s;
      }

      for (int s_B = 0; s_B < suffStatsCount_BLEU; ++s_B) {
        stats[d][s] = stats_BLEU[d][s_B];
        ++s;
      }
    }

    return stats;

  }


  public double score(int[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ") in TERMinusBLEU.score(int[])");
      System.exit(1);
    }

    double sc = 0.0;

    int[] stats_TER = new int[suffStatsCount_TER];
    int[] stats_BLEU = new int[suffStatsCount_BLEU];
    for (int s = 0; s < suffStatsCount_TER; ++s) { stats_TER[s] = stats[s]; }
    for (int s = 0; s < suffStatsCount_BLEU; ++s) { stats_BLEU[s] = stats[s+suffStatsCount_TER]; }

    double sc_T = myTER.score(stats_TER);
    double sc_B = myBLEU.score(stats_BLEU);

    sc = sc_T - sc_B;

    return sc;
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner)
  {
    int[] stats_TER = new int[suffStatsCount_TER];
    int[] stats_BLEU = new int[suffStatsCount_BLEU];
    for (int s = 0; s < suffStatsCount_TER; ++s) { stats_TER[s] = stats[s]; }
    for (int s = 0; s < suffStatsCount_BLEU; ++s) { stats_BLEU[s] = stats[s+suffStatsCount_TER]; }

    System.out.println("---TER---");
    myTER.printDetailedScore_fromStats(stats_TER, oneLiner);
    System.out.println("---BLEU---");
    myBLEU.printDetailedScore_fromStats(stats_BLEU, oneLiner);
    System.out.println("---------");
    System.out.println("  => " + metricName + " = " + score(stats));
  }

}

