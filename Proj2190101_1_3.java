import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Proj2190101_1_3 {

    private static String FILE = "time_series_covid19_confirmed_global.csv";
    private static String OUTFILE = "1_3.csv";

    private static String[] countriesWrongName = {"Holy See", "Korea, South", "Taiwan*"};
    private static String[] countriesReplaceName = {"Vatican City", "South Korea", "Taiwan"};

    public static CountryInfected[] getInfected() {

        ArrayList<CountryInfected> countryInfectedList = new ArrayList<>();

        BufferedReader csvReader = null;
        try {
            csvReader = new BufferedReader(new FileReader(FILE));
        } catch (FileNotFoundException e) {
            System.out.println(FILE + " not found");
            System.exit(1);
        }
        try {
            String row = csvReader.readLine();
            String prevCountry = "";
            int index;

            while ((row = csvReader.readLine()) != null) {
                List<String> data = parse(row);
                index = 4;

                String state = data.get(0);
                String country = data.get(1);

                if (Arrays.asList(countriesWrongName).contains(country)) {
                    country = countriesReplaceName[Arrays.asList(countriesWrongName).indexOf(country)];
                }

                int[] infected = new int[data.size() - 4];
                if (country.equals(prevCountry)) {
                    infected = countryInfectedList.get(countryInfectedList.size() - 1).getInfected();
                    countryInfectedList.remove(countryInfectedList.get(countryInfectedList.size() - 1));
                }

                for (int i = 4; i < data.size(); i++) {
                    infected[i - 4] += Integer.parseInt(data.get(i));
                }
                CountryInfected countryInfected = new CountryInfected(country, infected);
                countryInfectedList.add(countryInfected);

                prevCountry = country;
            }
            csvReader.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        countryInfectedList = checkTotal(countryInfectedList);

        CountryInfected[] result = countryInfectedList.toArray(new CountryInfected[0]);
        return result;
    }

    private static ArrayList<CountryInfected> checkTotal(ArrayList<CountryInfected> countryInfectedList) {

        ArrayList<CountryInfected> finalList = new ArrayList<>();
        for (CountryInfected countryInfected : countryInfectedList) {
            boolean remove = true;
            for (int confirmedCase : countryInfected.getInfected()) {
                if (confirmedCase > 99) {
                    remove = false;
                    break;
                }
            }
            if (!remove) {
                finalList.add(countryInfected);
            }
        }
        return finalList;
    }

    private static List<String> parse(String line)
            throws Exception {

        List<String> result = new ArrayList<>();

        boolean inQuotes = false;
        String field = "";

        for (char c : line.toCharArray()) {

            if (c == '"') {
                inQuotes = !inQuotes;
            } else {
                if (c == ',' && !inQuotes) { 
                    result.add(field.toString());
                    field="";            
                } else {
                    field+=String.valueOf(c);                
                }
            }

        }
        result.add(field.toString());          

        return result;

    }

    private static int[] getCountryInfected(String country, CountryInfected[] countryInfectedList) {
        for (CountryInfected countryInfected : countryInfectedList) {
            if (country.equals(countryInfected.getCountry())) {
                return countryInfected.getInfected();
            }
        }
        return null;
    }

    public static double[] getDoNothingCurve(int[] pastData, int numFutureDays) {

        if (pastData.length < 5) {
            System.out.println("You must have at least past data for 5 days");
            return null;
        }
        double[] tempArray = new double[numFutureDays + 4];
        int pastDataLastIndex = pastData.length - 1;
        
        tempArray[0] = pastData[pastDataLastIndex - 3]-pastData[pastDataLastIndex - 4];
        tempArray[1] = pastData[pastDataLastIndex - 2]-pastData[pastDataLastIndex - 3];
        tempArray[2] = pastData[pastDataLastIndex - 1]-pastData[pastDataLastIndex - 2];
        tempArray[3] = pastData[pastDataLastIndex ]-pastData[pastDataLastIndex-1];

        double[] result = new double[numFutureDays];
        for (int i = 0; i < numFutureDays; i++) {
            tempArray[i + 4] = tempArray[i + 3] * ((tempArray[i + 3] / tempArray[i + 2]) + (tempArray[i + 2] / tempArray[i + 1]) + (tempArray[i + 1] / tempArray[i])) / 3;
            result[i] = tempArray[i + 3]+tempArray[i + 4];
        }

        return result;
    }

    public static double[] getSCurve(int[] pastData, int numFutureDays, double[] paramLowerBounds, double[] paramUpperBounds) {
        double[] result = new double[numFutureDays];
        double minError = Double.MAX_VALUE;
        double optS = paramLowerBounds[0];
        double optD = paramLowerBounds[1];
        double optL = paramLowerBounds[2];
        double optM = paramLowerBounds[3];

        for (double S = paramLowerBounds[0]; S < paramUpperBounds[0]; S = S + (paramUpperBounds[0] - paramLowerBounds[0]) / 100) {
            for (double D = paramLowerBounds[1]; D < paramUpperBounds[1]; D = D + (paramUpperBounds[1] - paramLowerBounds[1]) / 20) {
                for (double L = paramLowerBounds[2]; L < paramUpperBounds[2]; L = L + (paramUpperBounds[2] - paramLowerBounds[2]) / 10) {
                    for (double M = paramLowerBounds[3]; M < paramUpperBounds[3]; M = M + (paramUpperBounds[3] - paramLowerBounds[3]) / 500) {
                        double[] projected = new double[pastData.length];
                        for (int d = 0; d < pastData.length; d++) {
                            projected[d] = S + (M / (1 + Math.exp(-1 * L * (d - D))));
                        }
                        //calculate MSE
                        double total = 0.0;
                        for (int d = 0; d < pastData.length; d++) {
                            total += ((projected[d] - pastData[d]) * (projected[d] - pastData[d]));
                        }
                        double error = total / pastData.length;
                        if (error < minError) {
                            minError = error;
                            optS = S;
                            optL = L;
                            optD = D;
                            optM = M;
                        }
                    }
                }
            }
        }
        //System.out.println("Error: "+minError);
        System.out.println("The fitted S-curve model has S=" + optS +
                ", D=" + optD + ",L=" + optL + ", M=" + optM + ", with the first projected day being d=" + (pastData.length + 1));
        System.out.println();

        int fistDay = pastData.length + 1;

        for (int i = 0; i < numFutureDays; i++) {
            result[i] = optS + (optM / (1 + Math.exp(-1 * optL * (fistDay + i - optD))));

        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        CountryInfected[] countryInfectedList = getInfected();
        FileWriter myWriter = new FileWriter(OUTFILE);

        int[] franceInfected = getCountryInfected("France", countryInfectedList);
        int[] germanyInfected = getCountryInfected("Germany", countryInfectedList);
        int[] netherlandsInfected = getCountryInfected("Netherlands", countryInfectedList);

        if (franceInfected != null) {
            double[] paramLowerBounds=new double[]{1350000.0,50.0,0.02,6000000};
            double[] paramUpperBounds=new double[]{1400000.0,70.0,0.9,7000000};
            System.out.println("FRANCE");
            double[] franceSCurve=getSCurve(Arrays.copyOfRange(franceInfected,franceInfected.length-121,franceInfected.length-1),90,paramLowerBounds,paramUpperBounds);
            writeResult(myWriter, franceSCurve);
            double[] franceDoNothing = getDoNothingCurve(franceInfected, 90);
            writeResult(myWriter, franceDoNothing);
        } else {
            System.out.println("France data could not be found");
        }

        if (germanyInfected!= null) {
            double[] paramLowerBounds=new double[]{1500000.0,80.0,0.02,3200000};
            double[] paramUpperBounds=new double[]{1600000.0,100.0,0.9,4000000};
            System.out.println("GERMANY");
            double[] germanySCurve=getSCurve(Arrays.copyOfRange(germanyInfected,germanyInfected.length-121,germanyInfected.length-1),90,paramLowerBounds,paramUpperBounds);
            writeResult(myWriter, germanySCurve);
            double[] germanyDoNothing = getDoNothingCurve(germanyInfected, 90);
            writeResult(myWriter, germanyDoNothing);
        } else {
            System.out.println("Germany data could not be found");
        }

        if (netherlandsInfected != null) {
            double[] paramLowerBounds=new double[]{750000,70.0,0.02,1200000};
            double[] paramUpperBounds=new double[]{800000.0,90.0,0.9,1500000};
            System.out.println("NETHERLANDS");
            double[] netherlandsSCurve=getSCurve(Arrays.copyOfRange(netherlandsInfected,netherlandsInfected.length-121,netherlandsInfected.length-1),90,paramLowerBounds,paramUpperBounds);
            writeResult(myWriter, netherlandsSCurve);
            double[] netherlandsDoNothing = getDoNothingCurve(netherlandsInfected, 90);
            writeResult(myWriter, netherlandsDoNothing);
        } else {
            System.out.println("Netherlands data could not be found");
        }

        myWriter.close();
    }

    private static void writeResult(FileWriter outputFile, double[] valueDoNothing) {

        try {
            String line="";
            for (double value : valueDoNothing) {
                line=line+String.valueOf(value)+",";
            }
            line=line.substring(0,line.length()-2);
            outputFile.write(line+"\n");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }


}
