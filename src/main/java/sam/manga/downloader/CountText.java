package sam.manga.downloader;

import javafx.scene.text.Text;

public class CountText extends Text {
    private int[] counts;
    private int sum;
    
    public void setSize(int size) {
        counts = new int[size];
        sum = 0;
    }
    public void set(int index, int value) {
        int old = counts[index]; 
        if(old == value)
            return;
        
        sum = sum - old + value;
        setText(String.valueOf(sum));
        counts[index] = value;
    }
    public int get(int index) {
        return counts[index];
    }
}
