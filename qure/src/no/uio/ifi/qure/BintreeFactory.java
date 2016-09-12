package no.uio.ifi.qure;

import java.util.List;
import java.util.Comparator;

public interface BintreeFactory {

    public String toString();

    public Bintree makeEmpty();

    public Bintree makeTop();

    public Bintree[] makeNDistinct(int n);

    // public Bintree makeAboveAprox(Space g, Space universe, int level);

    // public Bintree makeBelowAprox(Space g, Space universe, int level);

   /**
   * Gives a lexicographic total order on bintrees, used for sorting.
   * Blocks will be sorted according to z-order.
   */
   // public default Comparator<Bintree> getComparator() {
   //      return new Comparator<Bintree>() {
   //          public int compare(Bintree b1, Bintree b2) {
   //              if (b1.isEmpty())
   //                  return (b2.isEmpty()) ? 0 : -1;
   //              else if (b1.isTop())
   //                  return (b2.isTop()) ? 0 : 1;
   //              else if (b2.isEmpty())
   //                  return 1;
   //              else if (b2.isTop())
   //                  return -1;
   //              else {
   //                  int leftCT = compare(b1.left(), b2.left());
   //                  return (leftCT == 0) ? compare(b1.right(), b2.right()) : leftCT;
   //              }
   //          }
   //      };
   //  }
}
