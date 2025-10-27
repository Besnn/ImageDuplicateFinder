package hash;
import java.awt.image.BufferedImage;

public class PHashDct implements Hasher {
    public long hash(BufferedImage img) {
        int N = 32;
        double[][] vals = new double[N][N];
        for (int y=0;y<N;y++)
            for (int x=0;x<N;x++)
                vals[y][x] = (img.getRGB(x,y)&0xff)/255.0;

        double[][] dct = dct(vals);
        double[] low = new double[64];
        int idx=0;
        for (int y=0;y<8;y++) for (int x=0;x<8;x++)
            low[idx++] = dct[y][x];
        double mean = java.util.Arrays.stream(low,1,64).average().orElse(0); // skip DC
        long bits=0;
        for (int i=0;i<64;i++)
            if (low[i] > mean) bits |= (1L << i);
        return bits;
    }

    private static double[][] dct(double[][] f) {
        int N=f.length;
        double[][] F = new double[N][N];
        for (int u=0;u<N;u++)
            for (int v=0;v<N;v++) {
                double sum=0;
                for (int x=0;x<N;x++)
                    for (int y=0;y<N;y++)
                        sum += f[x][y]*Math.cos(((2*x+1)*u*Math.PI)/(2*N))*Math.cos(((2*y+1)*v*Math.PI)/(2*N));
                double cu = u==0 ? Math.sqrt(1.0/N) : Math.sqrt(2.0/N);
                double cv = v==0 ? Math.sqrt(1.0/N) : Math.sqrt(2.0/N);
                F[u][v] = cu*cv*sum;
            }
        return F;
    }

    public String name() { return "pHash"; }
}
