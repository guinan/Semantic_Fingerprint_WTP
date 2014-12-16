package utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A class providing an iterator over all choices of a certain number of 
 * elements from a given set. For a set S with n = |S|, there are are 
 * n!/(k!*(n-k)!) ways of choosing k elements from the set. This is 
 * the number of possible samples when doing sampling without 
 * replacement. Example:<br />
 * <pre>
 * S = { A,B,C,D }, n = |S| = 4
 * k = 2 
 * m = n!/(k!*(n-k)!) = 6
 * 
 * Choices:
 * [A, B]
 * [A, C]
 * [A, D]
 * [B, C]
 * [B, D]
 * [C, D]
 * </pre>
 * 
 * @author http://www.java-forum.org/members/Marco13.html
 * @author Christian Nywelt
 */
public class PowerSetGenerator<T> implements Iterable<T[]> {
    private final T input[];
    private final int sampleSize;
    private final int numElements;
 
    /**
	 * @return the numElements
	 */
	public int getNumElements() {
		return numElements;
	}
	
	/**
     * Creates an iterable over all choices of 'sampleSize' 
     * elements taken from the given array.
     * 
     * @param sampleSize
     * @param input
     */
    @SuppressWarnings("unchecked")
	public PowerSetGenerator(int sampleSize, ArrayList<T> input) {
    	this.sampleSize = sampleSize;
    	
        this.input = (T[]) java.lang.reflect.Array.newInstance(
            input.getClass().getComponentType(), input.size());
    	int i = 0;
    	for (T e : input) {
    		this.input[i++] = e;
    	}
    	numElements = factorial(this.input.length) / (factorial(sampleSize) * factorial(this.input.length - sampleSize));
    }
    
    /**
     * 
     * @param sampleSize
     * @param input
     * @param classOfT
     */
    @SuppressWarnings("unchecked")
	public PowerSetGenerator(int sampleSize, Collection<T> input, Class<T> classOfT) {
    	this.sampleSize = sampleSize;
    	
        this.input = (T[]) java.lang.reflect.Array.newInstance(
            classOfT, input.size());
    	int i = 0;
    	for (T e : input) {
    		this.input[i++] = e;
    	}
    	numElements = factorial(this.input.length) / (factorial(sampleSize) * factorial(this.input.length - sampleSize));
    }
    
    /**
     * Creates an iterable over all choices of 'sampleSize' 
     * elements taken from the given array.
     *  
     * @param sampleSize
     * @param input
     */
    public PowerSetGenerator(int sampleSize, T... input)
    {
        this.sampleSize = sampleSize;
        this.input = input.clone();
        numElements = nCr(input.length, sampleSize);
    }
    
    public static void main(String[] args) {
    	System.out.println(nCr(100, 5));
    }
    
    /**
     * 
     * @param n
     * @param k
     * @return
     */
    private static int nCr(int n, int k) {
    	if (k == 0) return 1;
    	if (2*k > n) {
    		return nCr(n, n-k);
    	} else {
    		int res = n-k+1;
    		for (int i = 2; i <= k; i++) {
    			res *= (n-k+i);
    			res /= i;
    		}
    		return res;
    	}
    }
    
    /**
     * 
     */
    public class PowerSetIterator implements Iterator<T[]>
    {
        private int current = 0;
        private int chosen[];
        
        // data
        private final int sampleSize;
        private final int numElements;
        private final int inputLength;
        
        /**
         * 
         * @param sampleSize
         * @param numElements
         */
        public PowerSetIterator(int sampleSize, int numElements, int inputLength) {
        	this.sampleSize = sampleSize;
        	this.numElements = numElements;
        	this.inputLength = inputLength;
        	this.chosen = new int[sampleSize];
        	// init first set
        	for (int i = 0; i < sampleSize; i++)
            {
                chosen[i] = i;
            }
        }
        
        /**
         * can be used to get the progress
         * @return
         */
        public int getCurrentItemIndex() {
        	return current;
        }

        public boolean hasNext()
        {
            return current < numElements;
        }

        public T[] next()
        {
        	//int res[] = chosen.clone();
        	// get current 
        	@SuppressWarnings("unchecked")
			T[] res = (T[]) java.lang.reflect.Array.newInstance(input.getClass().getComponentType(), sampleSize);
            for (int i = 0; i < chosen.length; i++) {
            	res[i] = input[chosen[i]];
            }
        	
        	// increase to next
        	current++;
            if (current < numElements)
            {
                increase(sampleSize - 1, inputLength - 1);
            }
            
            return res;
        }

        private void increase(int n, int max)
        {
            // The fist choice when choosing 3 of 5 elements consists
            // of 0,1,2. Subsequent choices are created by increasing
            // the last element of this sequence:
            // 0,1,3
            // 0,1,4
            // until the last element of the choice has reached the
            // maximum value. Then, the earlier elements of the 
            // sequence are increased recursively, while obeying the 
            // maximum value each element may have so that there may 
            // still be values assigned to the subsequent elements.
            // For the example: 
            // - The element with index 2 may have maximum value 4.
            // - The element with index 1 may have maximum value 3.
            // - The element with index 0 may have maximum value 2.
            // Each time that the value of one of these elements is
            // increased, the subsequent elements will simply receive
            // the subsequent values.
            if (chosen[n] < max)
            {
                chosen[n]++;
                for (int i = n + 1; i < sampleSize; i++)
                {
                    chosen[i] = chosen[i - 1] + 1;
                }
            }
            else
            {
                increase(n - 1, max - 1);
            }
        }

        public void remove()
        {
            throw new UnsupportedOperationException(
                "May not remove elements from a choice");
        }
    }
    
    /**
     * The Iterator
     */
    public PowerSetIterator iterator()
    {
        return new PowerSetIterator(sampleSize, numElements, input.length);
    }
    
    
    /**
     * Utility method for computing the factorial n! of a number n. 
     * The factorial of a number n is n*(n-1)*(n-2)*...*1, or more
     * formally:<br />
     * 0! = 1 <br />
     * 1! = 1 <br />
     * n! = n*(n-1)!<br />
     * 
     * @param n The number of which the factorial should be computed
     * @return The factorial, i.e. n!
     */
    public static int factorial(int n)
    {
        int f = 1;
        for (int i = 2; i <= n; i++)
        {
            f *= i;
        }
        return f;
    }
}