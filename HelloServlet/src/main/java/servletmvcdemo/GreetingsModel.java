package servletmvcdemo;

public class GreetingsModel {
	
	public static String getGreetingsInLanguage(String country) {
		String res = "";
		if ("Japan".equals(country)) {
			res = "Kon'nichiwa! Hōmon shite itadaki arigatōgozaimasu.";
		} else if ("Germany".equals(country)) {
			res = "Hallo! Danke für Ihren Besuch.";
		} else if ("India".equals(country)) {
			res = "हैलो! आने के लिए धन्यवाद।";
		} else if ("USA".equals(country)) { 
			res = "Hello! Thanks for visiting.";
		} else if ("Italy".equals(country)) { 
			res = "Ciao! Grazie per la visita.";
		} 
		return res;
	}

}
