/*
 * Copyright Takuya OOURA, 1996-2001 
 *
 * You may use, copy, modify and distribute this code
 * for any purpose (include commercial use) and without fee.
 * Please refer to this package when you modify this code. 
 */

package vavi.util;


/**
 * Fast Fourier/Cosine/Sine Transform.
 * <pre>
 *  dimension   :one
 *  data length :power of 2
 *  decimation  :frequency
 *  radix       :<b>split-radix</b>
 *  data        :inplace
 *  table       :use
 * </pre>
 * <h4>Appendix:</h4>
 * <p>
 *  The cos/sin table is recalculated when the larger table required.
 *  w[] and ip[] are compatible with all routines.
 * </p>
 * @author <a href="mailto:ooura@mmm.t.u-tokyo.ac.jp">Takuya OOURA</a>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060127 nsano port to java version <br>
 */
public class SplitRadixFft {

    /**
     * Complex Discrete Fourier Transform.
     * <pre>
     *  [definition]
     *      &lt;case1&gt;
     *          X[k] = sum_j=0&amp;&circ;n-1 x[j]*exp(2*pi*i*j*k/n), 0&lt;=k&lt;n
     *      &lt;case2&gt;
     *          X[k] = sum_j=0&amp;&circ;n-1 x[j]*exp(-2*pi*i*j*k/n), 0&lt;=k&lt;n
     *      (notes: sum_j=0&amp;&circ;n-1 is a summation from j=0 to n-1)
     *  [usage]
     *      &lt;case1&gt;
     *          ip[0] = 0; // first time only
     *          cdft(2*n, 1, a, ip, w);
     *      &lt;case2&gt;
     *          ip[0] = 0; // first time only
     *          cdft(2*n, -1, a, ip, w);
     *  [remark]
     *      Inverse of 
     *          cdft(2*n, -1, a, ip, w);
     *      is 
     *          cdft(2*n, 1, a, ip, w);
     *          for (j = 0; j &lt;= 2 * n - 1; j++) {
     *              a[j] *= 1.0 / n;
     *          }
     *      .
     * </pre>
     * @param n 2*n data length (int)
     *          n &gt;= 1, n = power of 2
     * @param isgn
     * @param a a[0...2*n-1] input/output data (REAL *)
     *                      input data
     *                          a[2*j] = Re(x[j]), 
     *                          a[2*j+1] = Im(x[j]), 0&lt;=j&lt;n
     *                      output data
     *                          a[2*k] = Re(X[k]), 
     *                          a[2*k+1] = Im(X[k]), 0&lt;=k&lt;n
     * @param ip ip[0...*] work area for bit reversal (int *)
     *           length of ip &gt;= 2+sqrt(n)
     *           strictly, 
     *           length of ip &gt;= 
     *              2+(1&lt;&lt;(int)(log(n+0.5)/log(2))/2).
     *           ip[0],ip[1] are pointers of the cos/sin table.
     * @param w w[0...n/2-1] cos/sin table (REAL *)
     *          w[],ip[] are initialized if ip[0] == 0.
     */
    public void cdft(int n, int isgn, double[] a, int[] ip, double[] w) {
        if (n > (ip[0] << 2)) {
            makewt(n >> 2, ip, w);
        }
        if (n > 4) {
            if (isgn >= 0) {
                bitrv2(n, ip, 2, a);
                cftfsub(n, a, w);
            } else {
                bitrv2conj(n, ip, 2, a);
                cftbsub(n, a, w);
            }
        } else if (n == 4) {
            cftbsub(n, a, w);
        }
    }

    /**
     * Real Discrete Fourier Transform.
     * <pre>    
     *  [definition]
     *      &lt;case1&gt; RDFT
     *          R[k] = sum_j = 0 &amp; &circ; (n - 1) a[j] * cos(2 * pi * j * k / n), 0 &lt;= k &lt;= n / 2
     *          I[k] = sum_j = 0 &amp; &circ; (n - 1) a[j] * sin(2 * pi * j * k / n), 0 &lt; k &lt; n / 2
     *      &lt;case2&gt; IRDFT (excluding scale)
     *          a[k] = (R[0] + R[n / 2] * cos(pi * k)) / 2 + 
     *              sum_j = 1 &amp; &circ; (n / 2 - 1) R[j] * cos(2 * pi * j * k / n) + 
     *              sum_j = 1 &amp; &circ; (n / 2 - 1) I[j] * sin(2 * pi * j * k / n), 0 &lt;= k &lt; n
     *  [usage]
     *      &lt;case1&gt;
     *          ip[0] = 0; // first time only
     *          rdft(n, 1, a, ip, w);
     *      &lt;case2&gt;
     *          ip[0] = 0; // first time only
     *          rdft(n, -1, a, ip, w);
     *  [remark]
     *      Inverse of 
     *          rdft(n, 1, a, ip, w);
     *      is 
     *          rdft(n, -1, a, ip, w);
     *          for (j = 0; j &lt;= n - 1; j++) {
     *              a[j] *= 2.0 / n;
     *          }
     *      .
     * </pre>    
     * @param n data length <br>
     *  n &gt;= 2, n = power of 2
     * @param isgn
     * @param a [0...n-1] input/output data
     * <pre>
     *  &lt;case1&gt;
     *      output data
     *          a[2 * k] = R[k], 0 &lt;= k &lt; n / 2
     *          a[2 * k + 1] = I[k], 0 &lt; k &lt; n / 2
     *          a[1] = R[n/2]
     *  &lt;case2&gt;
     *      input data
     *          a[2 * j] = R[j], 0 &lt;= j &lt; n / 2
     *          a[2 * j + 1] = I[j], 0 &lt; j &lt; n / 2
     *          a[1] = R[n / 2]
     * </pre>
     * @param ip [0...*] work area for bit reversal
     * <pre>
     *  length of ip &gt;= 2 + sqrt(n / 2)
     *  strictly, 
     *  length of ip &gt;= 
     *      2 + (1 &lt;&lt; (int) (log(n / 2 + 0.5) / log(2)) / 2).
     * </pre>
     *  ip[0],ip[1] are pointers of the cos/sin table.
     * @param w [0...n/2-1] cos/sin table <br>
     *  w[],ip[] are initialized if ip[0] == 0.
     */
    public void rdft(int n, int isgn, double[] a, int[] ip, double[] w) {
        int nw, nc;
        double xi;

        nw = ip[0];
        if (n > (nw << 2)) {
            nw = n >> 2;
            makewt(nw, ip, w);
        }
        nc = ip[1];
        if (n > (nc << 2)) {
            nc = n >> 2;
            makect(nc, ip, w, nw);
        }
        if (isgn >= 0) {
            if (n > 4) {
                bitrv2(n, ip, 2, a);
                cftfsub(n, a, w);
                rftfsub(n, a, nc, w, nw);
            } else if (n == 4) {
                cftfsub(n, a, w);
            }
            xi = a[0] - a[1];
            a[0] += a[1];
            a[1] = xi;
        } else {
            a[1] = 0.5 * (a[0] - a[1]);
            a[0] -= a[1];
            if (n > 4) {
                rftbsub(n, a, nc, w, nw);
                bitrv2(n, ip, 2, a);
                cftbsub(n, a, w);
            } else if (n == 4) {
                cftbsub(n, a, w);
            }
        }
    }

    /**
     * Discrete Cosine Transform.
     * <pre>    
     *  [definition]
     *      &lt;case1&gt; IDCT (excluding scale)
     *          C[k] = sum_j=0&amp;&circ;n-1 a[j]*cos(pi*j*(k+1/2)/n), 0&lt;=k&lt;n
     *      &lt;case2&gt; DCT
     *          C[k] = sum_j=0&amp;&circ;n-1 a[j]*cos(pi*(j+1/2)*k/n), 0&lt;=k&lt;n
     *  [usage]
     *      &lt;case1&gt;
     *          ip[0] = 0; // first time only
     *          ddct(n, 1, a, ip, w);
     *      &lt;case2&gt;
     *          ip[0] = 0; // first time only
     *          ddct(n, -1, a, ip, w);
     *  [remark]
     *      Inverse of 
     *          ddct(n, -1, a, ip, w);
     *      is 
     *          a[0] *= 0.5;
     *          ddct(n, 1, a, ip, w);
     *          for (j = 0; j &lt;= n - 1; j++) {
     *              a[j] *= 2.0 / n;
     *          }
     *      .
     * </pre>    
     * @param n data length (int)
     * <pre>
     *  n &gt;= 2, n = power of 2
     * </pre>
     * @param isgn
     * @param a [0...n-1] input/output data (REAL *)
     * <pre>
     *  output data
     *      a[k] = C[k], 0&lt;=k&lt;n
     * </pre>
     * @param ip [0...*] work area for bit reversal (int *)
     * <pre>
     *  length of ip &gt;= 2+sqrt(n/2)
     *  strictly, 
     *  length of ip &gt;= 
     *      2+(1&lt;&lt;(int)(log(n/2+0.5)/log(2))/2).
     *  ip[0],ip[1] are pointers of the cos/sin table.
     * </pre>
     * @param w [0...n*5/4-1] cos/sin table (REAL *)
     * <pre>
     *  w[],ip[] are initialized if ip[0] == 0.
     * </pre>
     */
    public void ddct(int n, int isgn, double[] a, int[] ip, double[] w) {
        int j, nw, nc;
        double xr;

        nw = ip[0];
        if (n > (nw << 2)) {
            nw = n >> 2;
            makewt(nw, ip, w);
        }
        nc = ip[1];
        if (n > nc) {
            nc = n;
            makect(nc, ip, w, nw);
        }
        if (isgn < 0) {
            xr = a[n - 1];
            for (j = n - 2; j >= 2; j -= 2) {
                a[j + 1] = a[j] - a[j - 1];
                a[j] += a[j - 1];
            }
            a[1] = a[0] - xr;
            a[0] += xr;
            if (n > 4) {
                rftbsub(n, a, nc, w, nw);
                bitrv2(n, ip, 2, a);
                cftbsub(n, a, w);
            } else if (n == 4) {
                cftbsub(n, a, w);
            }
        }
        dctsub(n, a, nc, w, nw);
        if (isgn >= 0) {
            if (n > 4) {
                bitrv2(n, ip, 2, a);
                cftfsub(n, a, w);
                rftfsub(n, a, nc, w, nw);
            } else if (n == 4) {
                cftfsub(n, a, w);
            }
            xr = a[0] - a[1];
            a[0] += a[1];
            for (j = 2; j < n; j += 2) {
                a[j - 1] = a[j] - a[j + 1];
                a[j] += a[j + 1];
            }
            a[n - 1] = xr;
        }
    }

    /**
     * Discrete Sine Transform.
     * <pre>    
     *  [definition]
     *      &lt;case1&gt; IDST (excluding scale)
     *          S[k] = sum_j=1&circ;n A[j]*sin(pi*j*(k+1/2)/n), 0&lt;=k&lt;n
     *      &lt;case2&gt; DST
     *          S[k] = sum_j=0&circ;n-1 a[j]*sin(pi*(j+1/2)*k/n), 0&lt;k&lt;=n
     *  [usage]
     *      &lt;case1&gt;
     *          ip[0] = 0; // first time only
     *          ddst(n, 1, a, ip, w);
     *      &lt;case2&gt;
     *          ip[0] = 0; // first time only
     *          ddst(n, -1, a, ip, w);
     *  [remark]
     *      Inverse of 
     *          ddst(n, -1, a, ip, w);
     *      is 
     *          a[0] *= 0.5;
     *          ddst(n, 1, a, ip, w);
     *          for (j = 0; j &lt;= n - 1; j++) {
     *              a[j] *= 2.0 / n;
     *          }
     *      .
     * </pre>    
     * @param n data length (int)
     *                      n &gt;= 2, n = power of 2
     * @param isgn
     * @param a [0...n-1] input/output data (REAL *)
     *                      &lt;case1&gt;
     *                          input data
     *                              a[j] = A[j], 0&lt;j&lt;n
     *                              a[0] = A[n]
     *                          output data
     *                              a[k] = S[k], 0&lt;=k&lt;n
     *                      &lt;case2&gt;
     *                          output data
     *                              a[k] = S[k], 0&lt;k&lt;n
     *                              a[0] = S[n]
     * @param ip [0...*] work area for bit reversal (int *)
     *                      length of ip &gt;= 2+sqrt(n/2)
     *                      strictly, 
     *                      length of ip &gt;= 
     *                          2+(1&lt;&lt;(int)(log(n/2+0.5)/log(2))/2).
     *                      ip[0],ip[1] are pointers of the cos/sin table.
     * @param w [0...n*5/4-1] cos/sin table (REAL *)
     *                      w[],ip[] are initialized if ip[0] == 0.
     */
    public void ddst(int n, int isgn, double[] a, int[] ip, double[] w) {
        int j, nw, nc;
        double xr;

        nw = ip[0];
        if (n > (nw << 2)) {
            nw = n >> 2;
            makewt(nw, ip, w);
        }
        nc = ip[1];
        if (n > nc) {
            nc = n;
            makect(nc, ip, w, nw);
        }
        if (isgn < 0) {
            xr = a[n - 1];
            for (j = n - 2; j >= 2; j -= 2) {
                a[j + 1] = -a[j] - a[j - 1];
                a[j] -= a[j - 1];
            }
            a[1] = a[0] + xr;
            a[0] -= xr;
            if (n > 4) {
                rftbsub(n, a, nc, w, nw);
                bitrv2(n, ip, 2, a);
                cftbsub(n, a, w);
            } else if (n == 4) {
                cftbsub(n, a, w);
            }
        }
        dstsub(n, a, nc, w, nw);
        if (isgn >= 0) {
            if (n > 4) {
                bitrv2(n, ip, 2, a);
                cftfsub(n, a, w);
                rftfsub(n, a, nc, w, nw);
            } else if (n == 4) {
                cftfsub(n, a, w);
            }
            xr = a[0] - a[1];
            a[0] += a[1];
            for (j = 2; j < n; j += 2) {
                a[j - 1] = -a[j] - a[j + 1];
                a[j] -= a[j + 1];
            }
            a[n - 1] = -xr;
        }
    }

    /**
     * Cosine Transform of RDFT (Real Symmetric DFT).
     * <pre>    
     *  [definition]
     *      C[k] = sum_j=0&circ;n a[j]*cos(pi*j*k/n), 0&lt;=k&lt;=n
     *  [usage]
     *      ip[0] = 0; // first time only
     *      dfct(n, a, t, ip, w);
     *  [parameters]
     *  [remark]
     *      Inverse of 
     *          a[0] *= 0.5;
     *          a[n] *= 0.5;
     *          dfct(n, a, t, ip, w);
     *      is 
     *          a[0] *= 0.5;
     *          a[n] *= 0.5;
     *          dfct(n, a, t, ip, w);
     *          for (j = 0; j &lt;= n; j++) {
     *              a[j] *= 2.0 / n;
     *          }
     *      .
     * </pre>    
     * @param n data length - 1 (int)
     * <pre>    
     *  n &gt;= 2, n = power of 2
     * </pre>    
     * @param a [0...n] input/output data (REAL *)
     * <pre>    
     *  output data
     *      a[k] = C[k], 0&lt;=k&lt;=n
     * </pre>    
     * @param t [0...n/2] work area (REAL *)
     * @param ip [0...*] work area for bit reversal (int *)
     * <pre>    
     *  length of ip &gt;= 2+sqrt(n/4)
     *  strictly, 
     *  length of ip &gt;= 
     *      2+(1&lt;&lt;(int)(log(n/4+0.5)/log(2))/2).
     *  ip[0],ip[1] are pointers of the cos/sin table.
     * </pre>    
     * @param w [0...n*5/8-1] cos/sin table (REAL *)
     * <pre>    
     *  w[],ip[] are initialized if ip[0] == 0.
     * </pre>    
     */
    public void dfct(int n, double[] a, double[] t, int[] ip, double[] w) {
        int j, k, l, m, mh, nw, nc;
        double xr, xi, yr, yi;

        nw = ip[0];
        if (n > (nw << 3)) {
            nw = n >> 3;
            makewt(nw, ip, w);
        }
        nc = ip[1];
        if (n > (nc << 1)) {
            nc = n >> 1;
            makect(nc, ip, w, nw);
        }
        m = n >> 1;
        yi = a[m];
        xi = a[0] + a[n];
        a[0] -= a[n];
        t[0] = xi - yi;
        t[m] = xi + yi;
        if (n > 2) {
            mh = m >> 1;
            for (j = 1; j < mh; j++) {
                k = m - j;
                xr = a[j] - a[n - j];
                xi = a[j] + a[n - j];
                yr = a[k] - a[n - k];
                yi = a[k] + a[n - k];
                a[j] = xr;
                a[k] = yr;
                t[j] = xi - yi;
                t[k] = xi + yi;
            }
            t[mh] = a[mh] + a[n - mh];
            a[mh] -= a[n - mh];
            dctsub(m, a, nc, w, nw);
            if (m > 4) {
                bitrv2(m, ip, 2, a);
                cftfsub(m, a, w);
                rftfsub(m, a, nc, w, nw);
            } else if (m == 4) {
                cftfsub(m, a, w);
            }
            a[n - 1] = a[0] - a[1];
            a[1] = a[0] + a[1];
            for (j = m - 2; j >= 2; j -= 2) {
                a[2 * j + 1] = a[j] + a[j + 1];
                a[2 * j - 1] = a[j] - a[j + 1];
            }
            l = 2;
            m = mh;
            while (m >= 2) {
                dctsub(m, t, nc, w, nw);
                if (m > 4) {
                    bitrv2(m, ip, 2, t);
                    cftfsub(m, t, w);
                    rftfsub(m, t, nc, w, nw);
                } else if (m == 4) {
                    cftfsub(m, t, w);
                }
                a[n - l] = t[0] - t[1];
                a[l] = t[0] + t[1];
                k = 0;
                for (j = 2; j < m; j += 2) {
                    k += l << 2;
                    a[k - l] = t[j] - t[j + 1];
                    a[k + l] = t[j] + t[j + 1];
                }
                l <<= 1;
                mh = m >> 1;
                for (j = 0; j < mh; j++) {
                    k = m - j;
                    t[j] = t[m + k] - t[m + j];
                    t[k] = t[m + k] + t[m + j];
                }
                t[mh] = t[m + mh];
                m = mh;
            }
            a[l] = t[0];
            a[n] = t[2] - t[1];
            a[0] = t[2] + t[1];
        } else {
            a[1] = a[0];
            a[2] = t[0];
            a[0] = t[1];
        }
    }

    /**
     * Sine Transform of RDFT (Real Anti-symmetric DFT).
     * <pre>    
     *  [definition]
     *      S[k] = sum_j=1&amp;circ;n-1 a[j]*sin(pi*j*k/n), 0&lt;k&lt;n
     *  [usage]
     *      ip[0] = 0; // first time only
     *      dfst(n, a, t, ip, w);
     *  [remark]
     *      Inverse of 
     *          dfst(n, a, t, ip, w);
     *      is 
     *          dfst(n, a, t, ip, w);
     *          for (j = 1; j &lt;= n - 1; j++) {
     *              a[j] *= 2.0 / n;
     *          }
     *      .
     * </pre>    
     * @param n data length + 1 (int)
     * <pre>    
     *  n &gt;= 2, n = power of 2
     * </pre>    
     * @param a [0...n-1] input/output data (REAL *)
     * <pre>    
     *  output data
     *      a[k] = S[k], 0&lt;k&lt;n
     *      (a[0] is used for work area)
     * </pre>    
     * @param t [0...n/2-1] work area (REAL *)
     * @param ip [0...*] work area for bit reversal (int *)
     * <pre>    
     *  length of ip &gt;= 2+sqrt(n/4)
     *  strictly, 
     *  length of ip &gt;= 
     *      2+(1&lt;&lt;(int)(log(n/4+0.5)/log(2))/2).
     *  ip[0],ip[1] are pointers of the cos/sin table.
     * </pre>    
     * @param w [0...n*5/8-1] cos/sin table (REAL *)
     * <pre>    
     *  w[],ip[] are initialized if ip[0] == 0.
     * </pre>    
     */
    public void dfst(int n, double[] a, double[] t, int[] ip, double[] w) {
        int j, k, l, m, mh, nw, nc;
        double xr, xi, yr, yi;

        nw = ip[0];
        if (n > (nw << 3)) {
            nw = n >> 3;
            makewt(nw, ip, w);
        }
        nc = ip[1];
        if (n > (nc << 1)) {
            nc = n >> 1;
            makect(nc, ip, w, nw);
        }
        if (n > 2) {
            m = n >> 1;
            mh = m >> 1;
            for (j = 1; j < mh; j++) {
                k = m - j;
                xr = a[j] + a[n - j];
                xi = a[j] - a[n - j];
                yr = a[k] + a[n - k];
                yi = a[k] - a[n - k];
                a[j] = xr;
                a[k] = yr;
                t[j] = xi + yi;
                t[k] = xi - yi;
            }
            t[0] = a[mh] - a[n - mh];
            a[mh] += a[n - mh];
            a[0] = a[m];
            dstsub(m, a, nc, w, nw);
            if (m > 4) {
                bitrv2(m, ip, 2, a);
                cftfsub(m, a, w);
                rftfsub(m, a, nc, w, nw);
            } else if (m == 4) {
                cftfsub(m, a, w);
            }
            a[n - 1] = a[1] - a[0];
            a[1] = a[0] + a[1];
            for (j = m - 2; j >= 2; j -= 2) {
                a[2 * j + 1] = a[j] - a[j + 1];
                a[2 * j - 1] = -a[j] - a[j + 1];
            }
            l = 2;
            m = mh;
            while (m >= 2) {
                dstsub(m, t, nc, w, nw);
                if (m > 4) {
                    bitrv2(m, ip, 2, t);
                    cftfsub(m, t, w);
                    rftfsub(m, t, nc, w, nw);
                } else if (m == 4) {
                    cftfsub(m, t, w);
                }
                a[n - l] = t[1] - t[0];
                a[l] = t[0] + t[1];
                k = 0;
                for (j = 2; j < m; j += 2) {
                    k += l << 2;
                    a[k - l] = -t[j] - t[j + 1];
                    a[k + l] = t[j] - t[j + 1];
                }
                l <<= 1;
                mh = m >> 1;
                for (j = 1; j < mh; j++) {
                    k = m - j;
                    t[j] = t[m + k] + t[m + j];
                    t[k] = t[m + k] - t[m + j];
                }
                t[0] = t[m + mh];
                m = mh;
            }
            a[l] = t[0];
        }
        a[0] = 0;
    }

    // -------- initializing routines --------

    /** */
    private void makewt(int nw, int[] ip, double[] w) {
        int j, nwh;
        double delta, x, y;

        ip[0] = nw;
        ip[1] = 1;
        if (nw > 2) {
            nwh = nw >> 1;
            delta = Math.atan(1.0) / nwh;
            w[0] = 1;
            w[1] = 0;
            w[nwh] = Math.cos(delta * nwh);
            w[nwh + 1] = w[nwh];
            if (nwh > 2) {
                for (j = 2; j < nwh; j += 2) {
                    x = Math.cos(delta * j);
                    y = Math.sin(delta * j);
                    w[j] = x;
                    w[j + 1] = y;
                    w[nw - j] = y;
                    w[nw - j + 1] = x;
                }
                bitrv2(nw, ip, 2, w);
            }
        }
    }

    /** */
    private void makect(int nc, int[] ip, double[] c, int cP) {
        int j, nch;
        double delta;

        ip[1] = nc;
        if (nc > 1) {
            nch = nc >> 1;
            delta = Math.atan(1.0) / nch;
            c[cP + 0] = Math.cos(delta * nch);
            c[cP + nch] = 0.5 * c[cP + 0];
            for (j = 1; j < nch; j++) {
                c[cP + j] = 0.5 * Math.cos(delta * j);
                c[cP + nc - j] = 0.5 * Math.sin(delta * j);
            }
        }
    }

    // -------- child routines --------

    /**
     * 2nd
     */
    private final void bitrv2(int n, int[] ip, int ipP, double[] a) {
        int j, j1, k, k1, l, m, m2;
        double xr, xi, yr, yi;

        ip[ipP + 0] = 0;
        l = n;
        m = 1;
        while ((m << 3) < l) {
            l >>= 1;
            for (j = 0; j < m; j++) {
                ip[ipP + m + j] = ip[ipP + j] + l;
            }
            m <<= 1;
        }
        m2 = 2 * m;
        if ((m << 3) == l) {
            for (k = 0; k < m; k++) {
                for (j = 0; j < k; j++) {
                    j1 = 2 * j + ip[ipP + k];
                    k1 = 2 * k + ip[ipP + j];
                    xr = a[j1];
                    xi = a[j1 + 1];
                    yr = a[k1];
                    yi = a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                    j1 += m2;
                    k1 += 2 * m2;
                    xr = a[j1];
                    xi = a[j1 + 1];
                    yr = a[k1];
                    yi = a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                    j1 += m2;
                    k1 -= m2;
                    xr = a[j1];
                    xi = a[j1 + 1];
                    yr = a[k1];
                    yi = a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                    j1 += m2;
                    k1 += 2 * m2;
                    xr = a[j1];
                    xi = a[j1 + 1];
                    yr = a[k1];
                    yi = a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                }
                j1 = 2 * k + m2 + ip[ipP + k];
                k1 = j1 + m2;
                xr = a[j1];
                xi = a[j1 + 1];
                yr = a[k1];
                yi = a[k1 + 1];
                a[j1] = yr;
                a[j1 + 1] = yi;
                a[k1] = xr;
                a[k1 + 1] = xi;
            }
        } else {
            for (k = 1; k < m; k++) {
                for (j = 0; j < k; j++) {
                    j1 = 2 * j + ip[ipP + k];
                    k1 = 2 * k + ip[ipP + j];
                    xr = a[j1];
                    xi = a[j1 + 1];
                    yr = a[k1];
                    yi = a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                    j1 += m2;
                    k1 += m2;
                    xr = a[j1];
                    xi = a[j1 + 1];
                    yr = a[k1];
                    yi = a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                }
            }
        }
    }

    /**
     * 2nd
     */
    private final void bitrv2conj(int n, int[] ip, int ipP, double[] a) {
        int j, j1, k, k1, l, m, m2;
        double xr, xi, yr, yi;

        ip[ipP + 0] = 0;
        l = n;
        m = 1;
        while ((m << 3) < l) {
            l >>= 1;
            for (j = 0; j < m; j++) {
                ip[ipP + m + j] = ip[ipP + j] + l;
            }
            m <<= 1;
        }
        m2 = 2 * m;
        if ((m << 3) == l) {
            for (k = 0; k < m; k++) {
                for (j = 0; j < k; j++) {
                    j1 = 2 * j + ip[ipP + k];
                    k1 = 2 * k + ip[ipP + j];
                    xr = a[j1];
                    xi = -a[j1 + 1];
                    yr = a[k1];
                    yi = -a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                    j1 += m2;
                    k1 += 2 * m2;
                    xr = a[j1];
                    xi = -a[j1 + 1];
                    yr = a[k1];
                    yi = -a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                    j1 += m2;
                    k1 -= m2;
                    xr = a[j1];
                    xi = -a[j1 + 1];
                    yr = a[k1];
                    yi = -a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                    j1 += m2;
                    k1 += 2 * m2;
                    xr = a[j1];
                    xi = -a[j1 + 1];
                    yr = a[k1];
                    yi = -a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                }
                k1 = 2 * k + ip[ipP + k];
                a[k1 + 1] = -a[k1 + 1];
                j1 = k1 + m2;
                k1 = j1 + m2;
                xr = a[j1];
                xi = -a[j1 + 1];
                yr = a[k1];
                yi = -a[k1 + 1];
                a[j1] = yr;
                a[j1 + 1] = yi;
                a[k1] = xr;
                a[k1 + 1] = xi;
                k1 += m2;
                a[k1 + 1] = -a[k1 + 1];
            }
        } else {
            a[1] = -a[1];
            a[m2 + 1] = -a[m2 + 1];
            for (k = 1; k < m; k++) {
                for (j = 0; j < k; j++) {
                    j1 = 2 * j + ip[ipP + k];
                    k1 = 2 * k + ip[ipP + j];
                    xr = a[j1];
                    xi = -a[j1 + 1];
                    yr = a[k1];
                    yi = -a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                    j1 += m2;
                    k1 += m2;
                    xr = a[j1];
                    xi = -a[j1 + 1];
                    yr = a[k1];
                    yi = -a[k1 + 1];
                    a[j1] = yr;
                    a[j1 + 1] = yi;
                    a[k1] = xr;
                    a[k1 + 1] = xi;
                }
                k1 = 2 * k + ip[ipP + k];
                a[k1 + 1] = -a[k1 + 1];
                a[k1 + m2 + 1] = -a[k1 + m2 + 1];
            }
        }
    }

    /**
     * 2nd
     * @see #rdft(int, int, double[], int[], double[])
     * @see #ddct(int, int, double[], int[], double[])
     * @see #cdft(int, int, double[], int[], double[])
     * @see #ddst(int, int, double[], int[], double[])
     * @see #dfst(int, double[], double[], int[], double[])
     * @see #dfct(int, double[], double[], int[], double[])
     */
    private void cftfsub(int n, double[] a, double[] w) {
        int j, j1, j2, j3, l;
        double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;
        
        l = 2;
        if (n > 8) {
            cft1st(n, a, w);
            l = 8;
            while ((l << 2) < n) {
                cftmdl(n, l, a, w);
                l <<= 2;
            }
        }
        if ((l << 2) == n) {
            for (j = 0; j < l; j += 2) {
                j1 = j + l;
                j2 = j1 + l;
                j3 = j2 + l;
                x0r = a[j] + a[j1];
                x0i = a[j + 1] + a[j1 + 1];
                x1r = a[j] - a[j1];
                x1i = a[j + 1] - a[j1 + 1];
                x2r = a[j2] + a[j3];
                x2i = a[j2 + 1] + a[j3 + 1];
                x3r = a[j2] - a[j3];
                x3i = a[j2 + 1] - a[j3 + 1];
                a[j] = x0r + x2r;
                a[j + 1] = x0i + x2i;
                a[j2] = x0r - x2r;
                a[j2 + 1] = x0i - x2i;
                a[j1] = x1r - x3i;
                a[j1 + 1] = x1i + x3r;
                a[j3] = x1r + x3i;
                a[j3 + 1] = x1i - x3r;
            }
        } else {
            for (j = 0; j < l; j += 2) {
                j1 = j + l;
                x0r = a[j] - a[j1];
                x0i = a[j + 1] - a[j1 + 1];
                a[j] += a[j1];
                a[j + 1] += a[j1 + 1];
                a[j1] = x0r;
                a[j1 + 1] = x0i;
            }
        }
    }

    /**
     * 2nd
     * @see #rdft(int, int, double[], int[], double[])
     * @see #ddct(int, int, double[], int[], double[])
     * @see #cdft(int, int, double[], int[], double[])
     * @see #ddst(int, int, double[], int[], double[])
     */
    private void cftbsub(int n, double[] a, double[] w) {
        int j, j1, j2, j3, l;
        double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;
        
        l = 2;
        if (n > 8) {
            cft1st(n, a, w);
            l = 8;
            while ((l << 2) < n) {
                cftmdl(n, l, a, w);
                l <<= 2;
            }
        }
        if ((l << 2) == n) {
            for (j = 0; j < l; j += 2) {
                j1 = j + l;
                j2 = j1 + l;
                j3 = j2 + l;
                x0r = a[j] + a[j1];
                x0i = -a[j + 1] - a[j1 + 1];
                x1r = a[j] - a[j1];
                x1i = -a[j + 1] + a[j1 + 1];
                x2r = a[j2] + a[j3];
                x2i = a[j2 + 1] + a[j3 + 1];
                x3r = a[j2] - a[j3];
                x3i = a[j2 + 1] - a[j3 + 1];
                a[j] = x0r + x2r;
                a[j + 1] = x0i - x2i;
                a[j2] = x0r - x2r;
                a[j2 + 1] = x0i + x2i;
                a[j1] = x1r - x3i;
                a[j1 + 1] = x1i - x3r;
                a[j3] = x1r + x3i;
                a[j3 + 1] = x1i + x3r;
            }
        } else {
            for (j = 0; j < l; j += 2) {
                j1 = j + l;
                x0r = a[j] - a[j1];
                x0i = -a[j + 1] + a[j1 + 1];
                a[j] += a[j1];
                a[j + 1] = -a[j + 1] - a[j1 + 1];
                a[j1] = x0r;
                a[j1 + 1] = x0i;
            }
        }
    }

    /**
     * 3rd
     * @see #cftfsub(int, double[], double[]) 
     */
    private void cft1st(int n, double[] a, double[] w) {
        int j, k1, k2;
        double wk1r, wk1i, wk2r, wk2i, wk3r, wk3i;
        double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;
        
        x0r = a[0] + a[2];
        x0i = a[1] + a[3];
        x1r = a[0] - a[2];
        x1i = a[1] - a[3];
        x2r = a[4] + a[6];
        x2i = a[5] + a[7];
        x3r = a[4] - a[6];
        x3i = a[5] - a[7];
        a[0] = x0r + x2r;
        a[1] = x0i + x2i;
        a[4] = x0r - x2r;
        a[5] = x0i - x2i;
        a[2] = x1r - x3i;
        a[3] = x1i + x3r;
        a[6] = x1r + x3i;
        a[7] = x1i - x3r;
        wk1r = w[2];
        x0r = a[8] + a[10];
        x0i = a[9] + a[11];
        x1r = a[8] - a[10];
        x1i = a[9] - a[11];
        x2r = a[12] + a[14];
        x2i = a[13] + a[15];
        x3r = a[12] - a[14];
        x3i = a[13] - a[15];
        a[8] = x0r + x2r;
        a[9] = x0i + x2i;
        a[12] = x2i - x0i;
        a[13] = x0r - x2r;
        x0r = x1r - x3i;
        x0i = x1i + x3r;
        a[10] = wk1r * (x0r - x0i);
        a[11] = wk1r * (x0r + x0i);
        x0r = x3i + x1r;
        x0i = x3r - x1i;
        a[14] = wk1r * (x0i - x0r);
        a[15] = wk1r * (x0i + x0r);
        k1 = 0;
        for (j = 16; j < n; j += 16) {
            k1 += 2;
            k2 = 2 * k1;
            wk2r = w[k1];
            wk2i = w[k1 + 1];
            wk1r = w[k2];
            wk1i = w[k2 + 1];
            wk3r = wk1r - 2 * wk2i * wk1i;
            wk3i = 2 * wk2i * wk1r - wk1i;
            x0r = a[j] + a[j + 2];
            x0i = a[j + 1] + a[j + 3];
            x1r = a[j] - a[j + 2];
            x1i = a[j + 1] - a[j + 3];
            x2r = a[j + 4] + a[j + 6];
            x2i = a[j + 5] + a[j + 7];
            x3r = a[j + 4] - a[j + 6];
            x3i = a[j + 5] - a[j + 7];
            a[j] = x0r + x2r;
            a[j + 1] = x0i + x2i;
            x0r -= x2r;
            x0i -= x2i;
            a[j + 4] = wk2r * x0r - wk2i * x0i;
            a[j + 5] = wk2r * x0i + wk2i * x0r;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[j + 2] = wk1r * x0r - wk1i * x0i;
            a[j + 3] = wk1r * x0i + wk1i * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[j + 6] = wk3r * x0r - wk3i * x0i;
            a[j + 7] = wk3r * x0i + wk3i * x0r;
            wk1r = w[k2 + 2];
            wk1i = w[k2 + 3];
            wk3r = wk1r - 2 * wk2r * wk1i;
            wk3i = 2 * wk2r * wk1r - wk1i;
            x0r = a[j + 8] + a[j + 10];
            x0i = a[j + 9] + a[j + 11];
            x1r = a[j + 8] - a[j + 10];
            x1i = a[j + 9] - a[j + 11];
            x2r = a[j + 12] + a[j + 14];
            x2i = a[j + 13] + a[j + 15];
            x3r = a[j + 12] - a[j + 14];
            x3i = a[j + 13] - a[j + 15];
            a[j + 8] = x0r + x2r;
            a[j + 9] = x0i + x2i;
            x0r -= x2r;
            x0i -= x2i;
            a[j + 12] = -wk2i * x0r - wk2r * x0i;
            a[j + 13] = -wk2i * x0i + wk2r * x0r;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[j + 10] = wk1r * x0r - wk1i * x0i;
            a[j + 11] = wk1r * x0i + wk1i * x0r;
            x0r = x1r + x3i;
            x0i = x1i - x3r;
            a[j + 14] = wk3r * x0r - wk3i * x0i;
            a[j + 15] = wk3r * x0i + wk3i * x0r;
        }
    }

    /** */
    private final void cftmdl(int n, int l, double[] a, double[] w) {
        int j, j1, j2, j3, k, k1, k2, m, m2;
        double wk1r, wk1i, wk2r, wk2i, wk3r, wk3i;
        double x0r, x0i, x1r, x1i, x2r, x2i, x3r, x3i;
        
        m = l << 2;
        for (j = 0; j < l; j += 2) {
            j1 = j + l;
            j2 = j1 + l;
            j3 = j2 + l;
            x0r = a[j] + a[j1];
            x0i = a[j + 1] + a[j1 + 1];
            x1r = a[j] - a[j1];
            x1i = a[j + 1] - a[j1 + 1];
            x2r = a[j2] + a[j3];
            x2i = a[j2 + 1] + a[j3 + 1];
            x3r = a[j2] - a[j3];
            x3i = a[j2 + 1] - a[j3 + 1];
            a[j] = x0r + x2r;
            a[j + 1] = x0i + x2i;
            a[j2] = x0r - x2r;
            a[j2 + 1] = x0i - x2i;
            a[j1] = x1r - x3i;
            a[j1 + 1] = x1i + x3r;
            a[j3] = x1r + x3i;
            a[j3 + 1] = x1i - x3r;
        }
        wk1r = w[2];
        for (j = m; j < l + m; j += 2) {
            j1 = j + l;
            j2 = j1 + l;
            j3 = j2 + l;
            x0r = a[j] + a[j1];
            x0i = a[j + 1] + a[j1 + 1];
            x1r = a[j] - a[j1];
            x1i = a[j + 1] - a[j1 + 1];
            x2r = a[j2] + a[j3];
            x2i = a[j2 + 1] + a[j3 + 1];
            x3r = a[j2] - a[j3];
            x3i = a[j2 + 1] - a[j3 + 1];
            a[j] = x0r + x2r;
            a[j + 1] = x0i + x2i;
            a[j2] = x2i - x0i;
            a[j2 + 1] = x0r - x2r;
            x0r = x1r - x3i;
            x0i = x1i + x3r;
            a[j1] = wk1r * (x0r - x0i);
            a[j1 + 1] = wk1r * (x0r + x0i);
            x0r = x3i + x1r;
            x0i = x3r - x1i;
            a[j3] = wk1r * (x0i - x0r);
            a[j3 + 1] = wk1r * (x0i + x0r);
        }
        k1 = 0;
        m2 = 2 * m;
        for (k = m2; k < n; k += m2) {
            k1 += 2;
            k2 = 2 * k1;
            wk2r = w[k1];
            wk2i = w[k1 + 1];
            wk1r = w[k2];
            wk1i = w[k2 + 1];
            wk3r = wk1r - 2 * wk2i * wk1i;
            wk3i = 2 * wk2i * wk1r - wk1i;
            for (j = k; j < l + k; j += 2) {
                j1 = j + l;
                j2 = j1 + l;
                j3 = j2 + l;
                x0r = a[j] + a[j1];
                x0i = a[j + 1] + a[j1 + 1];
                x1r = a[j] - a[j1];
                x1i = a[j + 1] - a[j1 + 1];
                x2r = a[j2] + a[j3];
                x2i = a[j2 + 1] + a[j3 + 1];
                x3r = a[j2] - a[j3];
                x3i = a[j2 + 1] - a[j3 + 1];
                a[j] = x0r + x2r;
                a[j + 1] = x0i + x2i;
                x0r -= x2r;
                x0i -= x2i;
                a[j2] = wk2r * x0r - wk2i * x0i;
                a[j2 + 1] = wk2r * x0i + wk2i * x0r;
                x0r = x1r - x3i;
                x0i = x1i + x3r;
                a[j1] = wk1r * x0r - wk1i * x0i;
                a[j1 + 1] = wk1r * x0i + wk1i * x0r;
                x0r = x1r + x3i;
                x0i = x1i - x3r;
                a[j3] = wk3r * x0r - wk3i * x0i;
                a[j3 + 1] = wk3r * x0i + wk3i * x0r;
            }
            wk1r = w[k2 + 2];
            wk1i = w[k2 + 3];
            wk3r = wk1r - 2 * wk2r * wk1i;
            wk3i = 2 * wk2r * wk1r - wk1i;
            for (j = k + m; j < l + (k + m); j += 2) {
                j1 = j + l;
                j2 = j1 + l;
                j3 = j2 + l;
                x0r = a[j] + a[j1];
                x0i = a[j + 1] + a[j1 + 1];
                x1r = a[j] - a[j1];
                x1i = a[j + 1] - a[j1 + 1];
                x2r = a[j2] + a[j3];
                x2i = a[j2 + 1] + a[j3 + 1];
                x3r = a[j2] - a[j3];
                x3i = a[j2 + 1] - a[j3 + 1];
                a[j] = x0r + x2r;
                a[j + 1] = x0i + x2i;
                x0r -= x2r;
                x0i -= x2i;
                a[j2] = -wk2i * x0r - wk2r * x0i;
                a[j2 + 1] = -wk2i * x0i + wk2r * x0r;
                x0r = x1r - x3i;
                x0i = x1i + x3r;
                a[j1] = wk1r * x0r - wk1i * x0i;
                a[j1 + 1] = wk1r * x0i + wk1i * x0r;
                x0r = x1r + x3i;
                x0i = x1i - x3r;
                a[j3] = wk3r * x0r - wk3i * x0i;
                a[j3 + 1] = wk3r * x0i + wk3i * x0r;
            }
        }
    }

    /**
     * 2nd
     * @see #rdft(int, int, double[], int[], double[])
     * @see #ddct(int, int, double[], int[], double[])
     * @see #ddst(int, int, double[], int[], double[])
     * @see #dfst(int, double[], double[], int[], double[])
     * @see #dfct(int, double[], double[], int[], double[])
     */
    private void rftfsub(int n, double[] a, int nc, double[] c, int cP) {
        int j, k, kk, ks, m;
        double wkr, wki, xr, xi, yr, yi;

        m = n >> 1;
        ks = 2 * nc / m;
        kk = 0;
        for (j = 2; j < m; j += 2) {
            k = n - j;
            kk += ks;
            wkr = 0.5 - c[cP + nc - kk];
            wki = c[cP + kk];
            xr = a[j] - a[k];
            xi = a[j + 1] + a[k + 1];
            yr = wkr * xr - wki * xi;
            yi = wkr * xi + wki * xr;
            a[j] -= yr;
            a[j + 1] -= yi;
            a[k] += yr;
            a[k + 1] -= yi;
        }
    }

    /**
     * 2nd
     * @see #rdft(int, int, double[], int[], double[])
     * @see #ddct(int, int, double[], int[], double[])
     * @see #ddst(int, int, double[], int[], double[]) 
     */
    private void rftbsub(int n, double[] a, int nc, double[] c, int cP) {
        int j, k, kk, ks, m;
        double wkr, wki, xr, xi, yr, yi;

        a[1] = -a[1];
        m = n >> 1;
        ks = 2 * nc / m;
        kk = 0;
        for (j = 2; j < m; j += 2) {
            k = n - j;
            kk += ks;
            wkr = 0.5 - c[cP + nc - kk];
            wki = c[cP + kk];
            xr = a[j] - a[k];
            xi = a[j + 1] + a[k + 1];
            yr = wkr * xr + wki * xi;
            yi = wkr * xi - wki * xr;
            a[j] -= yr;
            a[j + 1] = yi - a[j + 1];
            a[k] += yr;
            a[k + 1] = yi - a[k + 1];
        }
        a[m + 1] = -a[m + 1];
    }

    /**
     * 2nd
     * @see #ddct(int, int, double[], int[], double[])
     * @see #dfct(int, double[], double[], int[], double[])
     */
    private void dctsub(int n, double[] a, int nc, double[] c, int cP) {
        int j, k, kk, ks, m;
        double wkr, wki, xr;

        m = n >> 1;
        ks = nc / n;
        kk = 0;
        for (j = 1; j < m; j++) {
            k = n - j;
            kk += ks;
            wkr = c[cP + kk] - c[cP + nc - kk];
            wki = c[cP + kk] + c[cP + nc - kk];
            xr = wki * a[j] - wkr * a[k];
            a[j] = wkr * a[j] + wki * a[k];
            a[k] = xr;
        }
        a[m] *= c[cP + 0];
    }

    /**
     * 2nd
     * @see #ddst(int, int, double[], int[], double[])
     * @see #dfst(int, double[], double[], int[], double[]) 
     */
    private void dstsub(int n, double[] a, int nc, double[] c, int cP) {
        int j, k, kk, ks, m;
        double wkr, wki, xr;

        m = n >> 1;
        ks = nc / n;
        kk = 0;
        for (j = 1; j < m; j++) {
            k = n - j;
            kk += ks;
            wkr = c[cP + kk] - c[cP + nc - kk];
            wki = c[cP + kk] + c[cP + nc - kk];
            xr = wki * a[k] - wkr * a[j];
            a[k] = wkr * a[k] + wki * a[j];
            a[j] = xr;
        }
        a[m] *= c[cP + 0];
    }
}

/* */
