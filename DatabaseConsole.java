import java.util.Scanner;

public class DatabaseConsole {
	public static void runConsole(DatabaseConnection db) {
		Scanner console = new Scanner(System.in);

		System.out.print("Welcome! Type h for help. ");
		System.out.print("db > ");

		String line = console.nextLine();
		String[] parts;
		String arg = "";

		while (line != null && !line.equals("q")) {
			parts = line.split("\\s+");

			if (line.indexOf(" ") > 0) {
				arg = line.substring(line.indexOf(" ")).trim();
			}

			switch (parts[0]) {
				case "h":
					printHelp();
					break;

				case "page":
					try {
						if (parts.length >= 2)
							db.printPage(Integer.parseInt(arg));
						else
							System.out.println("Require an argument for this command");
					} catch (Exception e) {
						System.out.println("Error! argument must be an integer");
					}
					break;

				case "allhosts":
					db.getAllHosts();
					break;

				case "hostid":
					try {
						if (parts.length >= 2)
							db.getHostByID(Integer.parseInt(arg));
						else
							System.out.println("Require an argument for this command");
					} catch (Exception e) {
						System.out.println("Error! argument must be an integer");
					}
					break;

				case "listingfinances":
					db.priceAndRevenueByNeighbourhood();
					break;

				case "cleanesthosts":
					try {
						if (parts.length >= 2)
							db.topCleanHosts(Integer.parseInt(arg));
						else
							System.out.println("Require an argument for this command");
					} catch (Exception e) {
						System.out.println("Error! argument must be an integer");
					}
					break;

				case "listingsandcollisions":
					db.listingsAndCollisions();
					break;

				case "propertyratings":
					db.propertyRating();
					break;

				case "propertyforneighbourhood":
					db.reviewsForPropertyType();
					break;

				case "amenities":
					db.getAmenities();
					break;

				case "neighbourhoodperborough":
					db.neighbourhoodsAndBoroughs();
					break;

				case "hostlistings":
					try {
						if (parts.length >= 2)
							db.listingsForHost(Integer.parseInt(arg));
						else
							System.out.println("Require an argument for this command");
					} catch (Exception e) {
						System.out.println("Error! argument must be an integer");
					}
					break;

				case "listingdetails":
					// should change how we show full information
					try {
						if (parts.length >= 2) {
							db.listingDetails(Integer.parseInt(arg));
						} else {
							System.out.println("Require an argument for this command");
						}
					} catch (Exception e) {
						System.out.println("Error! argument must be an integer");
					}
					;
					break;

				case "no2rank":
					db.pricingWithNO2();
					break;

				case "sl":
					db.filterListingSearch(arg);
					break;
					
				case "r":
					db.repopulateDatabase();
					break;

				case "d":
					db.deleteAll();
					break;

				// sample where the input is supposed to be a string
				// case "n":
				// if (parts.length >= 2)
				// db.demoTwo(arg);
				// else
				// System.out.println("Require an argument for this command");
				// break;

				default:
					System.out.println("Read the help with h, or find help somewhere else.");
			}

			System.out.print("db > ");
			line = console.nextLine();
		}

		console.close();
	}

	private static void printHelp() {
		System.out.println("---- start help ----- ");
		System.out.println("Airbnb database");
		System.out.println("Commands:");
		System.out.println("h - Get help");
		System.out.println("page <page> - Print a specific page number");
		System.out.println("");

		System.out.println("allhosts - Get all hosts");
		System.out.println("hostid <id> - Search for a host by id");
		System.out.println("listingfinances - Get financial info for all listings");
		System.out.println("cleanesthosts <limit> - Get the specified number of hosts with the highest cleanliness rating");
		System.out.println("listingsandcollisions - Listings and collision counts per borough");
		System.out.println("propertyratings - Reviews count and average rating per property type");
		System.out.println("propertyforneighbourhood - Reviews and rating by property type and neighbourhood");
		System.out.println("amenities - Get list of amenities and corresponding ID's");
		System.out.println("neighbourhoodperborogh - Get list of neighbourhoods for each borough");
		System.out.println("hostlistings <id> - Get list of listings from a given host id");
		System.out.println("listingdetails <id> - Get all information on a particular listing");
		System.out.println("no2rank - Nitrogen Dioxide's impact on listing prices by borough");
		System.out.println("sl <name_of_borough (underscore for space)> <amenities...> - Listings in a particular borough that have the requested amenities");

		System.out.println("");
		System.out.println("d - Delete all data from the database");
		System.out.println("r - Repopulate the database");
		System.out.println("q - Exit the program");
		System.out.println("---- end help ----- ");
	}
}
