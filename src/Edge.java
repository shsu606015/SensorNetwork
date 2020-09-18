public class Edge{

    private int tail;
    private int head;

    public Edge(int tail, int head, int sort) {
    	if (sort == 1) {
        	if (tail<=head) {
            	this.tail = tail;
            	this.head = head;
        	} else {
            	this.tail = head;
            	this.head = tail;
        	}
    	} else {
            this.tail = tail;
            this.head = head;
    	}
    }
    
    
    public int getTail(){
        return this.tail;
    }

    public int getHead(){
        return this.head;
    }

    @Override
    public boolean equals(Object o) {
        System.out.println("calling Edge's equals()");
        if(this.tail == ((Edge)o).getTail() && this.head == ((Edge)o).getHead()) {
            return true;
        } else if(this.tail == ((Edge)o).getHead() && this.head == ((Edge)o).getTail()) {
            return true;
        }
        return false;
    }

    @Override
    public String toString(){
        return "(" + tail + ", " + head + ")";
    }
}
