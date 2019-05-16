package au.edu.unimelb.rpadiscovery;

import org.jbpt.algo.tree.rpst.IRPSTNode;

import java.util.LinkedList;
import java.util.TreeMap;


public class SubPolygon {

    public static final int DETERMINISTIC_SEQUENCE=0;
    public static final int NOT_DETERMINISTIC_SEQUENCE=1;
    public static final int PARTIAL_DETERMINISTIC_SEQUENCE=2;
    private int type;
    private String name;
    private String polygonName;
    private IRPSTNode start;
    private IRPSTNode finish;
    private int size=0;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRPSTPolygonName(){
        if(name.contains("-")){
            return name.substring(0, name.indexOf('-'));
        }else
            return name;
    }

    /*public String getPolygonName() {
        return polygonName;
    }

    public void setPolygonName(String polygonName) {
        this.polygonName = polygonName;
    }*/

    public IRPSTNode getStart() {
        return start;
    }

    public void setStart(IRPSTNode start) {
        this.start = start;
    }

    public IRPSTNode getFinish() {
        return finish;
    }

    public void setFinish(IRPSTNode finish) {
        this.finish = finish;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    public String toString(){
        return "name: "+name+"  start: "+start.getLabel()+"  finish: "+finish.getLabel()+"   size: "+size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubPolygon that = (SubPolygon) o;

        if (size != that.size) return false;
        if (!start.getLabel().equals(that.start.getLabel())) return false;
        return finish.getLabel().equals(that.finish.getLabel());
    }

    public LinkedList<String> getRPSTNodeSubPolygonList( TreeMap<String, LinkedList<IRPSTNode>> labelRPSDNodeMap){
        LinkedList<String> RPSTNodeSubPolygonList=new LinkedList<>();
        String polygon=this.getRPSTPolygonName();
        boolean start = false;
        boolean finish = false;

        for (IRPSTNode node : labelRPSDNodeMap.get(polygon)) {
            if (node.getLabel().equals(this.getFinish().getLabel())) {
                finish = true;
            }
            if (node.getLabel().equals(this.getStart().getLabel())) {
                start = true;
            }
            if (start) {
                RPSTNodeSubPolygonList.add(node.getLabel());
            }
            if (finish) {
                break;
            }
        }
        return RPSTNodeSubPolygonList;
    }

}
