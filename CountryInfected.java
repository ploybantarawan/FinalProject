public class CountryInfected {
    private String country;
    private int[] infected;

    public CountryInfected(String country,int[] infected) {
        this.country = country;
        this.infected=infected;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int[] getInfected() {
        return infected;
    }

    public void setInfected(int[] infected) {
        this.infected = infected;
    }
}
