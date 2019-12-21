/**************************************************************************************************************
 * Quote.jar
 * Copyright 2019 Michael Fross, all rights reserved
 * 
 * Quote is a command line program that display stock quotes and index data.
 * 
 * License:  
 *  MIT License / https://opensource.org/licenses/MIT
 *  Please see included LICENSE.txt file for additional details
 *   
 ***************************************************************************************************************/

package org.fross.quote;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import org.fusesource.jansi.Ansi;
import gnu.getopt.Getopt;

/**
 * Main execution class
 *
 */
public class Main {
	// Class Constants
	public static String VERSION;
	public static final String PROPERTIES_FILE = "quote.properties";

	public static void main(String[] args) {
		int optionEntry;
		String iexCloudToken;

		// Process application level properties file
		// Update properties from Maven at build time:
		// https://stackoverflow.com/questions/3697449/retrieve-version-from-maven-pom-xml-in-code
		try {
			InputStream iStream = Main.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
			Properties prop = new Properties();
			prop.load(iStream);
			VERSION = prop.getProperty("Application.version");
		} catch (IOException ex) {
			Output.FatalError("Unable to read property file '" + PROPERTIES_FILE + "'", 3);
		}

		// Process Command Line Options and set flags where needed
		Getopt optG = new Getopt("quote", args, "Dcke:h?v");
		while ((optionEntry = optG.getopt()) != -1) {
			switch (optionEntry) {
			case 'D': // Debug Mode
				Debug.Enable();
				break;
			case 'c': // Configure
				Scanner scanner = new Scanner(System.in);
				Output.PrintColorln(Ansi.Color.WHITE, "Enter the IEXcloud.io Secret Token: ");
				iexCloudToken = scanner.next();
				Debug.Print("Setting Peference iexcloudtoken: " + iexCloudToken);
				Prefs.Set("iexcloudtoken", iexCloudToken);
				Output.PrintColorln(Ansi.Color.YELLOW, "IEXCloud.io Secret Token Set To: '" + Prefs.QueryString("iexcloudtoken") + "'");
				break;
			case 'e':
				Output.Println("Export Results - COMPLETE LATER");
				System.exit(0);
				break;
			case 'k':
				Output.Println("The Configured IEXCloud Secret Key: " + Prefs.QueryString("iexcloudtoken"));
				System.exit(0);
				break;
			case 'v':
				Output.Println("This version of Quote is: " + VERSION);
				System.exit(0);
			case '?': // Help
			case 'h':
				Help.Display();
				System.exit(0);
				break;
			default:
				Output.PrintError("Unknown Command Line Option: '" + (char) optionEntry + "'");
				Help.Display();
				System.exit(0);
				break;
			}
		}

		// Read the prefs and make sure that an API key has been entered with the -c
		// option
		iexCloudToken = Prefs.QueryString("iexcloudtoken");
		if (iexCloudToken == "Error") {
			Output.FatalError("No iexcloud.io secret token provided.  Use '-c' option to configure.", 1);
		}

		// Display the header
		Output.PrintColorln(Ansi.Color.CYAN, "\nQuote v" + VERSION + " Copyright 2019 by Michael Fross");
		Output.PrintColorln(Ansi.Color.CYAN, "-------------------------------------------------------------------------------");
		Output.PrintColorln(Ansi.Color.YELLOW, "Symbol   Current    Chng   Chng%  DayHigh   Daylow  52WHigh   52WLow     YTD");
		Output.PrintColorln(Ansi.Color.CYAN, "-------------------------------------------------------------------------------");

		// Build an array list of symbols entered in on the command line
		Debug.Print("Number of Symbols entered: " + (args.length - optG.getOptind()));
		ArrayList<String> symbolList = new ArrayList<String>();
		for (int i = optG.getOptind(); i < args.length; i++) {
			Debug.Print("Symbol entered on commandline: " + args[i]);
			symbolList.add(args[i]);
		}

		// Display the data for the symbols entered. If no symbols were entered, just
		// display the index data
		if (!symbolList.isEmpty()) {
			// Loop through each entered symbol and display it's data
			Iterator<String> j = symbolList.iterator();
			String currentSymbol = "";

			while (j.hasNext()) {
				currentSymbol = j.next();
				String[] result = QuoteOps.GetQuote(currentSymbol, Prefs.QueryString("iexcloudtoken"));
				String[] outString = new String[9];

				// Validate the provided quote is valid
				if (result[1] == "Error") {
					// Display error and skip to the next iteration
					Output.PrintColorln(Ansi.Color.BLUE, "'" + result[0] + "' is invalid");
					continue;
				}

				// Format the Output into an array
				// Symbol
				try {
					// Symbol
					outString[0] = String.format("%-8s", result[0]);

					// Current
					try {
						outString[1] = String.format("%,8.2f", Float.valueOf(result[1]));
					} catch (NumberFormatException Ex) {
						outString[1] = String.format("%8s", "-");
					}

					// Change Amount
					try {
						outString[2] = String.format("%+,8.2f", Float.valueOf(result[2]));
					} catch (NumberFormatException Ex) {
						outString[2] = String.format("%8s", "-");
					}

					// Change Percentage
					try {
						outString[3] = String.format("%+,7.2f%%", (Float.valueOf(result[3]) * 100));
					} catch (NumberFormatException Ex) {
						outString[3] = String.format("%8s", "-");
					}

					// Day High
					try {
						outString[4] = String.format("%,9.2f", Float.valueOf(result[4]));
					} catch (NumberFormatException Ex) {
						outString[4] = String.format("%8s", "-");
					}

					// Day Low
					try {
						outString[5] = String.format("%,9.2f", Float.valueOf(result[5]));
					} catch (NumberFormatException Ex) {
						outString[5] = String.format("%8s", "-");
					}

					// 52 Week High
					try {
						outString[6] = String.format("%,9.2f", Float.valueOf(result[6]));
					} catch (NumberFormatException Ex) {
						outString[6] = String.format("%8s", "-");
					}

					// 52 Week Low
					try {
						outString[7] = String.format("%,9.2f", Float.valueOf(result[7]));
					} catch (NumberFormatException Ex) {
						outString[7] = String.format("%8s", "-");
					}

					// Year to date
					try {
						outString[8] = String.format("%+,9.2f%%", (Float.valueOf(result[8]) * 100));
					} catch (NumberFormatException Ex) {
						outString[8] = String.format("%8s", "-");
					}

				} catch (Exception Ex) {
					Output.PrintColorln(Ansi.Color.RED, "Unknown Error Occured");
				}

				// Determine the color based on the change amount
				Ansi.Color outputColor = Ansi.Color.WHITE;
				if (Float.valueOf(result[2]) < 0) {
					outputColor = Ansi.Color.RED;
				}

				// Write the output to the screen
				for (int k = 0; k < outString.length; k++) {
					Output.PrintColor(outputColor, outString[k]);
				}

				// Start a new line for the next security
				Output.Println("");

			}

			Output.Println("");
		}

		// Display Index Output Header
		Output.PrintColorln(Ansi.Color.CYAN, "-------------------------------------------------------------------------------");
		Output.PrintColorln(Ansi.Color.YELLOW, "Symbol       Current      Chng      Chng%");
		Output.PrintColorln(Ansi.Color.CYAN, "-------------------------------------------------------------------------------");

		// Loop through the three indexes and display the results
		String[] indexList = { "DOW", "NASDAQ", "S&P" };
		try {
			for (int i = 0; i < indexList.length; i++) {

				// Download the web page and return the results array
				Debug.Print("Getting Index data for: " + indexList[i]);
				String[] result = QuoteOps.GetIndex(indexList[i]);

				// Determine the color based on the change amount
				Ansi.Color outputColor = Ansi.Color.WHITE;
				if (Float.valueOf(result[2]) < 0) {
					outputColor = Ansi.Color.RED;
				}

				// Format the Output
				// Index Name
				String[] outString = new String[4];
				outString[0] = String.format("%-10s", result[0]);
				// Current
				outString[1] = String.format("%,10.2f", Float.valueOf(result[1]));
				// Change Amount
				outString[2] = String.format("%+,10.2f", Float.valueOf(result[2]));
				// Change Percentage
				outString[3] = String.format("%+,10.2f%%", Float.valueOf(result[3]));

				// Display Index results to the string
				for (int k = 0; k < outString.length; k++) {
					Output.PrintColor(outputColor, outString[k]);
				}

				// Start a new line for the next index
				Output.Println("");
			}

			// Display
			DateFormat sdf = new SimpleDateFormat("MM/dd/yyyy  HH:mm:ss zzz");
			Output.PrintColorln(Ansi.Color.CYAN, "\nExecuted:  " + sdf.format(new Date()));

		} catch (Exception Ex) {
			Output.PrintColor(Ansi.Color.RED, "No Data");
		}

	}
}
