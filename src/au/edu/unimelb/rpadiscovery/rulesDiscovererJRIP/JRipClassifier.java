package au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP;


import au.edu.unimelb.rpadiscovery.Main;
import au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data.FeatureVector;
import au.edu.unimelb.rpadiscovery.Utils.Log;
import au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data.DeclareConstraint;
import au.edu.unimelb.rpadiscovery.rulesDiscovererJRIP.data.Rule;
import weka.classifiers.rules.JRip;
import weka.classifiers.rules.RuleStats;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;

import static au.edu.unimelb.rpadiscovery.Utils.Functions.tryParseDouble;
import static au.edu.unimelb.rpadiscovery.Utils.LogTAG.TAG_FIND_DEPENDENCIES;

public class JRipClassifier {
    public JRipClassifier() {

    }

    public LinkedList<Rule> classify(List<FeatureVector> featureVectors, String relationName, DeclareConstraint constraint, Double minNodeSize, Boolean pruning) {
        Log log = Log.getInstance();
        try {
            List<String> keys = new ArrayList<>(featureVectors.get(0).attributes.keySet());
            //System.out.println(keys);
            ArrayList<Attribute> attributes = new ArrayList<>();
            for (String attr : keys) {
               /* System.out.println(attr);
                for(FeatureVector featureVector: featureVectors){
                    System.out.print(featureVector.attributes.get(attr)+", ");
                }
                System.out.println();*/
                String value = firstValueNotNull(featureVectors, attr);

                if (tryParseDouble(value))
                    attributes.add(new Attribute(attr));
                else {
                    List<String> attVals = featureVectors.stream().map(obj -> obj.attributes.get(attr)).distinct().collect(Collectors.toList());

                    //System.out.println("attVals: "+attVals);
                    attributes.add(new Attribute(attr, attVals));
                }
            }
            List<String> labels = featureVectors.stream().map(obj -> obj.label).distinct().collect(Collectors.toList());

            //System.out.println(labels);
            attributes.add(new Attribute("Class", labels));

            Instances data = new Instances(relationName, attributes, featureVectors.size());

            for (int i = 0; i < featureVectors.size(); i++) {
                Instance inst = new DenseInstance(attributes.size());
                for (Attribute attr : attributes) {
                    if (!attr.name().equals("Class")) {
                        String value = featureVectors.get(i).attributes.get(attr.name());
                        if (attr.isNumeric())
                            inst.setValue(attr, Double.parseDouble(value));
                        else
                            inst.setValue(attr, value);
                    } else {
                        String value = featureVectors.get(i).label;
                        if (attr.isNumeric())
                            inst.setValue(attr, Double.parseDouble(value));
                        else
                            inst.setValue(attr, value);
                    }
                }
                data.add(inst);
            }

            if (data.classIndex() == -1)
                data.setClassIndex(data.numAttributes() - 1);

            //System.out.println("\n" + relationName + "\n");

            if (labels.size() == 1) {
                //TODO: cancel this print
                System.err.println("JRipClassifier -> in classify there is only 1 label target!!!!!");

                return null;
            } else {
                JRip classifier = new JRip();

               /* String[] options = {
                        "-F", "3",
                        "-N", "2.0",
                        "-O", "2",
                        "-S", "1",
                        "-P"
                };*/

                //TODO: SET PARAMETRES

                String[] options;
                if (!pruning) {
                    options = new String[3];
                    options[0] = "-N";
                    options[1] = "1.0";
                    options[2] = "-P";
                } else {
                    options = new String[2];
                    options[0] = "-N";
                    options[1] = String.valueOf((double) Math.round(minNodeSize * featureVectors.size()));
                }
                classifier.setOptions(options);
                try {
                    classifier.buildClassifier(data);
                }catch (Exception e){
                    e.printStackTrace();
                    return null;
                }

                RuleStats ruleStats = new RuleStats();
                ruleStats.setData(data);
                ruleStats.setRuleset(classifier.getRuleset());
                ruleStats.countData();

                if (classifier.getRuleset().size() > 0) {
                    String[] classifierRules = classifier.toString().split("\n"); //check rules..print this!
                    log.println(TAG_FIND_DEPENDENCIES, 7, Arrays.toString(classifierRules));
                    LinkedList<Rule> rules = new LinkedList<>();
                    for (int i = 3; i < classifierRules.length - 2; i++) {

                        String className = classifierRules[i].substring(classifierRules[i].lastIndexOf('=') + 1, classifierRules[i].lastIndexOf('(') - 1);
                        String antecedent = classifierRules[i].substring(0, classifierRules[i].indexOf(" =>"));
                        log.println(TAG_FIND_DEPENDENCIES, 7, "ANTECEDENT: *" + antecedent + "*");
                        double firstValue = Double.parseDouble(classifierRules[i].substring(classifierRules[i].lastIndexOf('(') + 1, classifierRules[i].lastIndexOf('/')));
                        double secondValue = Double.parseDouble(classifierRules[i].substring(classifierRules[i].lastIndexOf('/') + 1, classifierRules[i].lastIndexOf(')')));


                        if (!antecedent.equals("")) {

                            log.println(TAG_FIND_DEPENDENCIES, 7,classifierRules[i]);
                            log.println(TAG_FIND_DEPENDENCIES, 7,"first case");
                            rules.add(new Rule(antecedent, className, firstValue, secondValue));
                        } else {
                            if (classifier.getRuleset().size() == 1) {

                                System.err.println("find CONSTANT in JRipClassifier: " + classifierRules[i]);
                                //rules.add(new Rule(antecedent, className, firstValue, secondValue));
                            } else {
                                log.println(TAG_FIND_DEPENDENCIES, 7,"third case");
                                String defaultRule = "";
                                for (int j = 3; j < classifierRules.length - 3; j++) {
                                    if (j != i) {
                                        String value = classifierRules[j].substring(0, classifierRules[j].indexOf(" =>"));
                                        value = value.replaceAll("[)] and [(]", ") || (");
                                        value = value.replaceAll("[)(]", "");
                                        defaultRule += "(" + makeNegative(value) + ")" + " and ";
                                    }
                                }
                                if (defaultRule.length() > 0)
                                    defaultRule = defaultRule.substring(0, defaultRule.length() - 5);
                                defaultRule += classifierRules[i].substring(classifierRules[i].indexOf(" =>"), classifierRules[i].length() - classifierRules[i].indexOf(" =>"));


                                log.println(TAG_FIND_DEPENDENCIES, 7,"negative rule---> original: " + classifierRules[i] + "  makeNegative: " + defaultRule);
                                antecedent = defaultRule.substring(0, defaultRule.indexOf(" =>"));
                                log.println(TAG_FIND_DEPENDENCIES, 7,"new antecedent: *" + antecedent + "*");
                                rules.add(new Rule(antecedent, className, firstValue, secondValue));
                            }
                        }
                        log.println(TAG_FIND_DEPENDENCIES, 7);
                    }

                    return rules;
                }
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String firstValueNotNull(List<FeatureVector> featureVectors, String attr) {
        for (int i = 0; i < featureVectors.size(); i++) {
            String value = featureVectors.get(i).attributes.get(attr);
            if (value != null)
                return value;
        }
        return null;
    }

    public String makeNegative(String condition) {
        HashMap<String, String> map = new HashMap<>();
        map.put("<", ">=");
        map.put(">=", "<");
        map.put(">", "<=");
        map.put("<=", ">");
        map.put("=", "!=");
        map.put("!=", "=");

        StringBuilder sb = new StringBuilder();
        Scanner testScanner = new Scanner(condition);
        while (testScanner.hasNext()) {
            String text = testScanner.next();
            text = map.get(text) == null ? text : map.get(text);
            sb.append(text + " ");
        }
        return sb.toString().substring(0, sb.length() - 1);
    }

    public static HashMap<String, List<String>> getAntecedent(String rule) {
        HashMap<String, List<String>> antecedent = new HashMap<>();

        String[] q = rule.split(" => Class=");
        String q1 = q[0];
        String joinType = "";
        if (q1.contains(") and ("))
            joinType = "AND";
        else
            joinType = "OR";
        String[] ant = q1.split("[)] and [(]|[)] or [(]");
        for (int i = 0; i < ant.length; i++)
            ant[i] = ant[i].replaceAll("[)]|[(]", "");
        antecedent.put(joinType, Arrays.asList(ant));
        return antecedent;
    }

    public static HashMap<String, List<String>> getConsequent(String rule) {
        HashMap<String, List<String>> consequent = new HashMap<>();

        rule = rule.replaceAll("\\s\\([\\d\\.]+/[\\d\\.]+\\)", "");
        String[] q = rule.split(" => Class=");
        String q2 = q[1];
        String joinType = "";
        if (q2.contains(") and ("))
            joinType = "AND";
        else
            joinType = "OR";
        String[] csq = q2.split("[)] and [(]|[)] or [(]");
        for (int i = 0; i < csq.length; i++)
            csq[i] = csq[i].replaceAll("[)]|[(]", "");
        consequent.put(joinType, Arrays.asList(csq));
        return consequent;
    }
}