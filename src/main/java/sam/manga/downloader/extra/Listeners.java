package sam.manga.downloader.extra;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Consumer;

public class Listeners<T> {
    private T[] ts;
    
    @SuppressWarnings("unchecked")
    public Listeners(Class<T> t) {
        ts = (T[]) Array.newInstance(t.getComponentType(), 2);
    }

    public void apply(Consumer<T> consumer) {
        for (T t : ts) 
            if(t != null)
                consumer.accept(t); 
    }

    public void removeListener(T t) {
        for (int i = 0; i < ts.length; i++) {
            if(ts[i] == t) {
                ts[i] = null;
                break;
            }
        }
    }
    public void addListener(T t) {
        int n = -1;
        for (int i = 0; i < ts.length; i++) {
            T thist = ts[i];
            if(thist == t) 
                return;
            else if(thist == null)
                n = i;
        }
        if(n == -1) {
            ts = Arrays.copyOf(ts, ts.length + 1);
            ts[ts.length - 1] = t;
        } else {
            ts[n] = t;
        }
    }
}
