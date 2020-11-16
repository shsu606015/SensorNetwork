import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
public class SensorNetworkGraph extends JPanel implements Runnable {
	private static final long serialVersionUID = 1L;
	
	private Map<Integer, Axis> nodes;
	private double graphWidth;
    private double graphHeight;
    private int scaling = 25; // 25
    private int ovalSize = 10; // 6
    private int gridCount = 10; // 10
    private boolean connected;
    private Map<Integer, Set<Integer>> adjList;
    private int[] dataGens;

    public SensorNetworkGraph(int[] dataGens){
        this.dataGens = dataGens;
    }

    public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public Map<Integer, Set<Integer>> getAdjList() {
		return adjList;
	}

	public void setAdjList(Map<Integer, Set<Integer>> adjList) {
		this.adjList = adjList;
	}

	public void setNodes(Map<Integer, Axis> nodes) {
        this.nodes = nodes;
        invalidate();
        this.repaint();
    }

    public Map<Integer, Axis> getNodes() {
        return nodes;
    }
    
    public double getGraphWidth() {
		return graphWidth;
	}

	public void setGraphWidth(double graphWidth) {
		this.graphWidth = graphWidth;
	}

	public double getGraphHeight() {
		return graphHeight;
	}

	public void setGraphHeight(double graphHeight) {
		this.graphHeight = graphHeight;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
       super.paintComponent(g);
       Graphics2D g2 = (Graphics2D) g;
       g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

       double xScale =  ((getWidth() - 3 * scaling) / (graphWidth));
       double yScale =   (( getHeight() - 3 * scaling) / (graphHeight));
       
       List<Point> graphPoints = new ArrayList<Point>();
       for (Integer key: nodes.keySet()) {
           double x1 = ( nodes.get(key).getxAxis() * (xScale) + (2*scaling));
           double y1 =  ((graphHeight - nodes.get(key).getyAxis())  * yScale + scaling );
           Point point = new Point();
           point.setLocation(x1, y1);
           graphPoints.add(point);
       }
              
       g2.setColor(Color.white);
       g2.fillRect(2*scaling, scaling, getWidth() - (3 * scaling), getHeight() - 3 * scaling);
       g2.setColor(Color.black);

       for (int i = 0; i < gridCount + 1; i++) {
           int x0 = 2*scaling;
           int x1 = ovalSize + (2*scaling);
           int y0 = getHeight() - ((i * (getHeight() - (3*scaling))) / gridCount + (2*scaling));
           int y1 = y0;
           if (nodes.size() > 0) {
               g2.setColor(Color.black);
               g2.drawLine((2*scaling) + 1 + ovalSize, y0, getWidth() - scaling, y1);
               String yLabel = ((int) ((getGraphHeight() * ((i * 1.0) / gridCount)) * 100)) / 100.0 + "";
               FontMetrics metrics = g2.getFontMetrics();
               int labelWidth = metrics.stringWidth(yLabel);
               g2.drawString(yLabel, x0 - labelWidth - 5, y0 + (metrics.getHeight() / 2) - 3);
           }
           g2.drawLine(x0, y0, x1, y1);
       }

       for (int i = 0; i < gridCount + 1; i++) {
               int x0 = i * (getWidth() - (scaling * 3)) / gridCount+ (2*scaling);
               int x1 = x0;
               int y0 = getHeight() - (2*scaling);
               int y1 = y0 - ovalSize;
               //if ((i % ((int) ((nodes.size() / 20.0)) + 1)) == 0) {
               if (nodes.size() > 0) {
                   g2.setColor(Color.black);
                   g2.drawLine(x0, getHeight() - (2*scaling) - 1 - ovalSize, x1, scaling);
                   String xLabel = ((int) ((getGraphWidth() * ((i * 1.0) / gridCount)) * 100)) / 100.0 + "";//i + "";
                   FontMetrics metrics = g2.getFontMetrics();
                   int labelWidth = metrics.stringWidth(xLabel);
                   g2.drawString(xLabel, x0 - labelWidth / 2, y0 + metrics.getHeight() + 3);
                   
               }
               g2.drawLine(x0, y0, x1, y1);
           //}
       }

       //Draw the edges
       Stroke stroke = g2.getStroke();
       // pick color
       Color darkBlue = new Color(0, 10, 100); // Color white
       g2.setColor(darkBlue);
       // control the width "?? f"
       g2.setStroke(new BasicStroke(1f));
       for (int node: adjList.keySet()) {
    	   if((adjList.get(node) != null) && (!adjList.get(node).isEmpty())) {
               for (int adj: adjList.get(node)) {
                   if(adjList.get(node).contains(adj)) {
                           int x1 = graphPoints.get(node-1).x;
                           int y1 = graphPoints.get(node-1).y;
                           int x2 = graphPoints.get(adj-1).x;
                           int y2 = graphPoints.get(adj-1).y;
                           g2.drawLine(x1, y1, x2, y2);
                   }
               }
    	   }
       }

       //Draw the oval
       g2.setStroke(stroke);
       // pick color
       Color lightBlue = new Color(0, 110, 255); // Color white
       g2.setColor(lightBlue);
       for (int i = 0; i < graphPoints.size(); i++) {
           double x = graphPoints.get(i).x - ovalSize / 2;
           double y = graphPoints.get(i).y - ovalSize / 2;
           double ovalW = ovalSize;
           double ovalH = ovalSize;
           Ellipse2D.Double shape = new Ellipse2D.Double(x, y, ovalW, ovalH);

           boolean flag = false;
           for (int dg: dataGens){
               if(i+1==dg){
                   x = graphPoints.get(i).x;
                   y = graphPoints.get(i).y;
                   g2.fill(createDefaultStar(5, x, y));
                   g2.draw(createDefaultStar(5, x, y));
                   flag = true;
               }
           }

           if (!flag) {
               g2.fill(shape);
               g2.draw(shape);
           }
       }
       
       //Label the nodes
//       g2.setColor(Color.red);
//       for (int i = 0; i < graphPoints.size(); i++) {
//           int x = graphPoints.get(i).x - ovalSize / 2;
//           int y = graphPoints.get(i).y - ovalSize / 2;
//           g2.setFont(new Font("TimesRoman", Font.PLAIN, 24));
//           g2.drawString(""+(i+1), x, y);
//       }
   }

	public void run() {
		String graphName= "Sensor Network Graph";
		JFrame frame = new JFrame(graphName);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(this);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
   }

    private static Shape createDefaultStar(double radius, double centerX, double centerY) {
        return createStar(centerX, centerY, radius, radius * 2.63, 5,
                Math.toRadians(-18));
    }

    private static Shape createStar(double centerX, double centerY,
                                    double innerRadius, double outerRadius, int numRays,
                                    double startAngleRad) {
        Path2D path = new Path2D.Double();
        double deltaAngleRad = Math.PI / numRays;
        for (int i = 0; i < numRays * 2; i++)
        {
            double angleRad = startAngleRad + i * deltaAngleRad;
            double ca = Math.cos(angleRad);
            double sa = Math.sin(angleRad);
            double relX = ca;
            double relY = sa;
            if ((i & 1) == 0)
            {
                relX *= outerRadius;
                relY *= outerRadius;
            }
            else
            {
                relX *= innerRadius;
                relY *= innerRadius;
            }
            if (i == 0)
            {
                path.moveTo(centerX + relX, centerY + relY);
            }
            else
            {
                path.lineTo(centerX + relX, centerY + relY);
            }
        }
        path.closePath();
        return path;
    }
}