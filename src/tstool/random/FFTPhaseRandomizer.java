package tstool.random;

/**
 * Surogat data generator : FFT phase randomisation. 
 * 
 * The idea is simple. A Fourier-Transformation is
 * applied to a measured time series. Than, the 
 * resulting Fourier Coefficients are modified.
 * 
 * 
 * Fourier Transformed (FT) Surrogates (also known as Phase-randomized Surrogates)

These surrogate series are generated by randomizing the frequency domain phases 
of the original time series. Specifically achieved by Fourier-transforming the 
original series into the frequency domain and then multiplying the phase at each 
frequency by image002ao, where image004ao is a uniformly distributed random 
number in the range image006ao in such a way as to preserve symmetry ...
 
The inverse transform of these realizations are the desired target surrogates.

* 
 * The Fourier-Transformation is applied again and the
 * result is a random number series with long term 
 * correlations.
 * 
 * The main method is a Tester which shows DFA2 for 
 * multiple test series and a chart with the dependency
 * between alpha and beta, beside the theoretical 
 * curve.
 * 
 * SOURCE : 
 * 
 */

import statphys.detrending.*;
import chart.simple.MultiChart;
import data.series.Messreihe;
import data.series.MessreiheFFT;
import data.series.MesswertTabelle;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Vector;
import org.apache.commons.math.stat.regression.SimpleRegression;
import statphys.detrending.methods.IDetrendingMethod;
import stdlib.StdDraw;
import stdlib.StdStats;

public class FFTPhaseRandomizer {
    
    private static boolean debug = false;

    static double fitMIN = 1.2;
    static double fitMAX = 3.5;
    
    static Messreihe alphasCALC = new Messreihe();
    static Messreihe alphasTHEO = new Messreihe();
    static Vector<Messreihe> check = new Vector<Messreihe>();
    
    static Vector<Messreihe> tests = null;
    static StringBuffer log = null;
    
    static double[][] alphas = null; 
    static double[] betas = null; 
    
  
    
    // nur in dem Fall auch eine DFA Kurve zeigen
    static boolean firstInLoop = false;
    static int numberOfLoop = 0;
    static int numberOfBeta = 0;
 
    static int order = -1; // is defined in method: getRandomRow() ...
    
    
    /**
     *   Parameters with influence on RUNTIME and PERFORMANCE
     */
    static public int nrOfSValues = 250;
    
    static int EXP = 16; // 2 ^ EXP = length of the row

    public static void main(String args[]) throws Exception {
        
        boolean showTest = true;

        // initilize the stdlib.StdRandom-Generator
        stdlib.StdRandom.initRandomGen(1);
        
        // prepare some data structures
        tests = new Vector<Messreihe>();
        log = new StringBuffer();
                
        log.append("fit range: [" + fitMIN + ", ..., " + fitMAX + " ]\n" );
        
        int Z = 15;
        double STRETCH = 4;

        int LOOPS = 10;
        
        alphas = new double[Z][LOOPS];
        betas = new double[Z]; 

        // double[] alphas = { 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3 };  

        int c = 0;
        for( int i=0; i < Z; i++ ) {
           
            if ( showTest ) { 
                System.out.println(">>> i=" +  i + "\n" );
            }
            
            for( int j = 0; j < LOOPS; j++ ) {  
                
                if ( j == 0 ) firstInLoop = true;
                else firstInLoop = false;
                numberOfLoop = j;
                
                Messreihe measure = getRandomRow( (int)Math.pow(2, EXP) );
                
                Messreihe mr = getPhaseRandomizedRow( measure, showTest, true, i );
            }    
            
            c = i;
        }
        
        c++;
        
        firstInLoop = true;
        Messreihe mr2 = getRandomRow( (int)Math.pow(2, EXP) );
        
        Messreihe mr = getPhaseRandomizedRow( mr2, showTest, true, c );
        
        boolean showLegend = true;
        MultiChart.open(tests, "Fluctuation Function F(s): DFA " + order, 
                        "log(s)", "log(F(s))", false, log.toString() );
        
        
        alphasCALC.setLabel("CALC");
        alphasTHEO.setLabel("Theorie");
        // check.add( alphasCALC );
        check.add( alphasTHEO );
        
        calcErrorChart( alphas, numberOfBeta, numberOfLoop );
        
        MultiChart.open(check, "alpha vs. beta", 
                        "beta", "alpha", showLegend );
        
        File f = new File( "./TESTDATA_PhaseRandom" );
        
        MesswertTabelle mwt = new MesswertTabelle( "random_numbers_CHECK" );
        
        mwt.createParrentFile( f );
        mwt.singleX = false;
        mwt.setMessReihen( check );
        mwt.setHeader("# random_numbers_CHECK");
        mwt.writeToFile();
        
        mwt = new MesswertTabelle( "random_numbers_FS" );
        
        mwt.createParrentFile( f );
        mwt.singleX = false;
        mwt.setMessReihen( tests );
        mwt.setHeader("# random_numbers_FS");
        mwt.writeToFile();
        
    }
    
     
    
    /**
     * 
     * @param length
     * @param beta
     * @param showTest
     * @return
     * @throws Exception 
     */
    public static Messreihe getRandomRow( int length ) throws Exception {
        // has toint N = length; by a multiple of 4 !!!
        int N = length;
        
  

        /**
         * Vorbedingungen:
         *
         * Eine Zeitreihe mit bereinigten Werten, ohne Lücken, ausser
         * es hat einen fahlichen Sinn, Lücken zu nutzen.
         */
        

        Messreihe d4 = Messreihe.getGaussianDistribution(N);
        
        return d4;
    }    
        
        
    public static Messreihe getPhaseRandomizedRow( Messreihe d4, boolean showTest, boolean runDFA, int testZaehler ) throws Exception {

        DecimalFormat df = new DecimalFormat("0.000");
        int N = d4.yValues.size();
        double[] zr = new double[N];

        // nun wird das Array mit den Daten der ZR übergeben
        MessreiheFFT mr4_NEW = MessreiheFFT.cnvert(d4);
        
        MessreiheFFT temp = mr4_NEW.getPhaseRandomizedModifiedFFT_INV();

        if ( runDFA ) {

            Vector<Messreihe> vr = new Vector<Messreihe>();
            Vector<Messreihe> v = new Vector<Messreihe>();
            vr.add( d4 );

            zr = d4.getData()[1];

            IDetrendingMethod dfa = DetrendingMethodFactory.getDetrendingMethod(DetrendingMethodFactory.DFA2);
            order = dfa.getPara().getGradeOfPolynom();
            dfa.getPara().setzSValues( nrOfSValues );

            // Anzahl der Werte in der Zeitreihe
            dfa.setNrOfValues(N);

            // die Werte für die Fensterbreiten sind zu wählen ...
            //dfa.initIntervalS();
            dfa.initIntervalSlog();
            if ( debug ) dfa.showS();


            // http://stackoverflow.com/questions/12049407/build-sample-data-for-apache-commons-fast-fourier-transform-algorithm

            dfa.setZR(temp.getData()[1]);

            dfa.calc();

            Messreihe mr4 = dfa.getResultsMRLogLog();
            mr4.setLabel( d4.getLabel() );
            v.add(mr4);

            String status = dfa.getStatus();

            SimpleRegression alphaSR = mr4.linFit(fitMIN, fitMAX);

            double alpha = alphaSR.getSlope();

            alphasCALC.addValuePair( testZaehler, alpha );

            if ( showTest ) {
                MultiChart.open(v, "fluctuation function F(s) [order:" + order + "] ", "log(s)", "log(F(s))", true, "alpha=" + alpha );
                if ( firstInLoop ) tests.add(mr4);

                try{
                    alphas[numberOfBeta][numberOfLoop] = alpha;  
                }
                catch(Exception ex) { 
                    
                }
                    
                System.out.println( " alpha = " + df.format( alpha ) );
                System.out.println( "       = " + ( (2 * alpha) - 1.0 ) );

            }

        }
        
        // System.out.println( temp.yValues.size() + " " + temp.getLabel() );
        return temp;
    }

    static Messreihe mrAV = new Messreihe();  // Mittelwerte
    static Messreihe mrSTD = new Messreihe();  // standardabweichung
    static Messreihe mrMAX = new Messreihe();  // ERROR
    static Messreihe mrMIN = new Messreihe();  // ERROR
        
    private static void calcErrorChart(double[][] d, int nrB, int nrLOOPS ) {
        
        mrAV = new Messreihe();  // Mittelwerte
        mrSTD = new Messreihe();  // standardabweichung
        mrMAX = new Messreihe();  // ERROR
        mrMIN = new Messreihe();  // ERROR
    
        mrAV.setLabel("mean");
        mrSTD.setLabel("std");
        mrMAX.setLabel("upper");
        mrMIN.setLabel("lower");
        
        for( int iB = 0; iB < nrB; iB++ ) { 
            
            double[] L = new double[nrLOOPS];
            
            // werte umlagern
            for( int iL = 0; iL < nrLOOPS; iL++ ) { 
                L[iL] = d[iB][iL];
            }
            try {
                double mean = stdlib.StdStats.mean(L);
                double std = stdlib.StdStats.stddev(L);

                mrAV.addValuePair(betas[iB] , mean );
                mrSTD.addValuePair(betas[iB] , std );

                mrMAX.addValuePair( betas[iB] , mean+std );
                mrMIN.addValuePair( betas[iB] , mean-std );
            }
            catch(Exception ex) { 
                System.err.println( ex.getMessage() );
                System.err.println( ex.getCause() );
                
            }
        }
        
        check.add( mrAV );        
        check.add( mrSTD );
        check.add( mrMAX );
        check.add( mrMIN );
        
    }
    

}
