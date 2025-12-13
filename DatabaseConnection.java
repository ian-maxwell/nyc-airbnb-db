import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseConnection {
	private Connection connection;
	private String header;
	private String divider;
	private ArrayList<String> output;

	public DatabaseConnection(String username, String password) {
		String connectionUrl =
			"jdbc:sqlserver://YOUR_AWS_ENDPOINT:1433;"
			+ "database=airbnb_db;"
			+ "user=" + username + ";"
			+ "password=" + password + ";"
			+ "encrypt=true;"
			+ "trustServerCertificate=true;"
			+ "loginTimeout=30;";

		try {
			connection = DriverManager.getConnection(connectionUrl);
			header = "";
			divider = "";
			output = new ArrayList<>();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void printPage(int page) {
		if (output.isEmpty()) {
			System.out.println("No results to display.");
			return;
		}

		int pageSize = 10;
		int totalPages = (int) Math.ceil((double) output.size() / pageSize);

		if (page < 1 || page > totalPages) {
			System.out.println("Page number out of range.");
			return;
		}

		System.out.println(divider);
		System.out.println(header);
		System.out.println(divider);

		int start = (page - 1) * pageSize;
		int end = Math.min(page * pageSize, output.size());
		for (int i = start; i < end; i++) {
			System.out.println(output.get(i));
		}

		System.out.println(divider);
		System.out.printf("Page %d of %d\n", page, totalPages);
	}

	public void getAllHosts() {
		try {
			String sql = """
				SELECT * FROM HOSTS;
			""";

			PreparedStatement statement = connection.prepareStatement(sql);

			System.out.println("Fetching Data...");
			ResultSet resultSet = statement.executeQuery();

			String headerFormat = "| %-9s | %-16.16s | %-36.36s | %-10s | %-29s |";
			String bodyFormat = "| %9d | %-16.16s | %-36.36s | %-10s | %-29s |";

			output.clear();
			header = String.format(headerFormat, "ID", "Name", "About", "Joined", "Neighbourhood");
			divider = "+-----------+------------------+--------------------------------------+------------+-------------------------------+";

			while (resultSet.next()) {
				int id = resultSet.getInt("host_id");
				String name = resultSet.getString("host_name");
				String about = resultSet.getString("host_about");
				String since = resultSet.getString("host_since");
				String neighbourhood = resultSet.getString("host_neighbourhood");

				if (name != null && name.length() > 16) {
					name = name.substring(0, 13);
					name += "...";
				}
				if (about != null && about.length() > 36) {
					about = about.substring(0, 33);
					about += "...";
				}

				output.add(String.format(bodyFormat, id, name, about, since, neighbourhood));
			}

			resultSet.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void getHostByID(int input) {
		// input comes in as int to prevent injection
		try {
			String sql = """
				SELECT * FROM HOSTS WHERE host_id = ?;
			""";

			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setInt(1, input);

			System.out.println("Fetching Data...");
			ResultSet resultSet = statement.executeQuery();

			String headerFormat = "| %-6.6s | %-9.9s | %-9.9s | %-10s | %-8.8s | %-18s | %-7s | %-9s | %-12s |";
			String bodyFormat = "| %6.6s | %-9.9s | %-9.9s | %-10s | %-8.8s | %-18s | %7s | %9s | %-12s |";

			output.clear();
			header = String.format(headerFormat, "ID", "Name", "About", "Joined", "Hood", "Response Time", "Resp. %", "Accept. %", "Is Superhost");
			divider = "+--------+-----------+-----------+------------+----------+--------------------+---------+-----------+--------------+";

			while (resultSet.next()) {
				int id = resultSet.getInt("host_id");
				String idStr = Integer.toString(id);
				String name = resultSet.getString("host_name");
				String about = resultSet.getString("host_about");
				String since = resultSet.getString("host_since");
				String neighbourhood = resultSet.getString("host_neighbourhood");
				String respTime = resultSet.getString("host_response_time");
				int respRate = resultSet.getInt("host_response_rate");
				String respRateStr = respRate + "%";
				int acceptRate = resultSet.getInt("host_acceptance_rate");
				String acceptRateStr = acceptRate + "%";
				String superhost = resultSet.getString("host_is_superhost");

				if (idStr.length() > 6) {
					idStr = idStr.substring(0, 3);
					idStr += "...";
				}

				if (name != null && name.length() > 12) {
					name = name.substring(0, 9);
					name += "...";
				}

				if (about != null && about.length() > 13) {
					about = about.substring(0, 10);
					about += "...";
				}

				if (neighbourhood != null && neighbourhood.length() > 12) {
					neighbourhood = neighbourhood.substring(0, 9);
					neighbourhood += "...";
				}

				if (superhost.equals("t")) {
					superhost = "Yes";
				} else if (superhost.equals("f")) {
					superhost = "No";
				}

				output.add(String.format(bodyFormat, id, name, about, since, neighbourhood, respTime, respRateStr, acceptRateStr, superhost));
			}

			resultSet.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void priceAndRevenueByNeighbourhood() {
		try {
			String sql = """
				SELECT listing_neighbourhood, avg(price) as avg_price, sum(revenue_l365d) as total_revenue
				FROM Listings
				JOIN Neighbourhoods on neighbourhood = listing_neighbourhood
				WHERE price IS NOT NULL
				GROUP BY listing_neighbourhood
				ORDER BY total_revenue DESC;
			""";

			PreparedStatement statement = connection.prepareStatement(sql);

			System.out.println("Fetching Data...");
			ResultSet resultSet = statement.executeQuery();

			String headerFormat = "| %-25s | %-13s | %-13s |";
			String bodyFormat = "| %-25s | %13s | %13s |";

			output.clear();
			header = String.format(headerFormat, "Neighbourhood", "Average Price", "Total Revenue");
			divider = "+---------------------------+---------------+---------------+";

			while (resultSet.next()) {
				String neighbourhood = resultSet.getString("listing_neighbourhood");
				float avgPrice = resultSet.getFloat("avg_price");
				String avgPriceStr = "$" + String.format("%.2f", avgPrice);
				int revenue = resultSet.getInt("total_revenue");
				String revenueStr = "$" + revenue;

				output.add(String.format(bodyFormat, neighbourhood, avgPriceStr, revenueStr));
			}

			resultSet.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void topCleanHosts(int input) {
		try {
			String sql = """
				SELECT TOP (?) Hosts.host_id, Hosts.host_name, avg(Listings.cleanliness_rating) AS avg_cleanliness
				FROM Hosts
				JOIN Listings ON Hosts.host_id = Listings.host_id
				WHERE Listings.cleanliness_rating IS NOT NULL
				GROUP BY Hosts.host_id, Hosts.host_name
				ORDER BY avg_cleanliness DESC;
			""";

			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setInt(1, input);

			System.out.println("Fetching Data...");
			ResultSet resultSet = statement.executeQuery();

			String headerFormat = "| %-9s | %-42.42s | %-26s |";
			String bodyFormat = "| %-9s | %-42.42s | %26.2f |";

			output.clear();
			header = String.format(headerFormat, "ID", "Name", "Average Cleanliness Rating");
			divider = "+-----------+--------------------------------------------+----------------------------+";

			while (resultSet.next()) {
				int id = resultSet.getInt("host_id");
				String name = resultSet.getString("host_name");
				float avgRating = resultSet.getFloat("avg_cleanliness");

				if (name != null && name.length() > 39) {
					name = name.substring(0, 36);
					name += "...";
				}

				output.add(String.format(bodyFormat, id, name, avgRating));
			}

			resultSet.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void listingsAndCollisions() {
		try {
			String sql = """
				WITH count_listings AS (
					SELECT n.borough, COUNT(l.listing_id) AS listing_count
					FROM Neighbourhoods n
					JOIN Listings l ON n.neighbourhood = l.listing_neighbourhood
					GROUP BY n.borough
				),
				count_collisions AS (
					SELECT n.borough, COUNT(c.collision_id) AS collision_count
					FROM Neighbourhoods n
					JOIN Collisions c ON n.borough = c.borough
					GROUP BY n.borough
				)
				SELECT cl.borough, cl.listing_count, cc.collision_count
				FROM count_listings cl
				JOIN count_collisions cc ON cl.borough = cc.borough;
			""";

			PreparedStatement statement = connection.prepareStatement(sql);

			System.out.println("Fetching Data...");
			ResultSet resultSet = statement.executeQuery();

			String headerFormat = "| %-16s | %-16s | %-16s |";
			String bodyFormat = "| %-16s | %-16d | %-16d |";

			output.clear();
			header = String.format(headerFormat, "Borough", "Listing Count", "Collision Count");
			divider = "+------------------+------------------+------------------+";

			while (resultSet.next()) {
				String borough = resultSet.getString("borough");
				int listings = resultSet.getInt("listing_count");
				int collisions = resultSet.getInt("collision_count");

				output.add(String.format(bodyFormat, borough, listings, collisions));
			}

			resultSet.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void propertyRating() {
		try {
			String sql = """
				SELECT l.property_type, COUNT(r.review_id) AS review_count, AVG(l.rating) AS avg_rating
				FROM Listings l
				JOIN Reviews r ON l.listing_id = r.listing_id
				GROUP BY l.property_type
				ORDER BY review_count DESC;
			""";

			PreparedStatement statement = connection.prepareStatement(sql);

			System.out.println("Fetching Data...");
			ResultSet resultSet = statement.executeQuery();

			String headerFormat = "| %-40s | %-16s | %-16s |";
			String bodyFormat = "| %-40s | %16d | %16.2f |";

			output.clear();
			header = String.format(headerFormat, "Property Type", "Total Reviews", "Average Rating");
			divider = "+------------------------------------------+------------------+------------------+";

			while (resultSet.next()) {
				String type = resultSet.getString("property_type");
				int reviews = resultSet.getInt("review_count");
				float avg = resultSet.getFloat("avg_rating");

				output.add(String.format(bodyFormat, type, reviews, avg));
			}

			resultSet.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void reviewsForPropertyType() {
		try {
			String sql = """
				SELECT l.property_type, l.listing_neighbourhood, COUNT(r.review_id) AS review_count, AVG(l.rating) AS avg_rating
				FROM Listings l
				JOIN Reviews r ON l.listing_id = r.listing_id
				JOIN Neighbourhoods n ON l.listing_neighbourhood = n.neighbourhood
				GROUP BY l.property_type, l.listing_neighbourhood
				ORDER BY review_count DESC;
			""";

			PreparedStatement statement = connection.prepareStatement(sql);

			System.out.println("Fetching Data...");
			ResultSet resultSet = statement.executeQuery();

			String headerFormat = "| %-40s | %-32s | %-16s | %-16s |";
			String bodyFormat = "| %-40s | %-32s | %16d | %16.2f |";

			output.clear();
			header = String.format(headerFormat, "Property Type", "Neighbourhood", "Review Count", "Average Rating");
			divider = "+------------------------------------------+----------------------------------+------------------+------------------+";

			while (resultSet.next()) {
				String type = resultSet.getString("property_type");
				String hood = resultSet.getString("listing_neighbourhood");
				int reviews = resultSet.getInt("review_count");
				float avg = resultSet.getFloat("avg_rating");

				output.add(String.format(bodyFormat, type, hood, reviews, avg));
			}

			resultSet.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void getAmenities() {
		try {
			String sql = "SELECT * FROM Amenities";

			String headerFormat = "| %-8s | %-48s |";
			String bodyFormat = "| %8d | %-48s |";

			PreparedStatement statement = connection.prepareStatement(sql);

			System.out.println("Fetching Data...");
			ResultSet result = statement.executeQuery();

			output.clear();
			header = String.format(headerFormat, "ID", "Amenity Name");
			divider = "+----------+--------------------------------------------------+";

			while (result.next()) {
				int amenity_id = result.getInt("amenity_id");
				String amenity_name = result.getString("amenity_name");

				output.add(String.format(bodyFormat, amenity_id, amenity_name));
			}

			result.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void neighbourhoodsAndBoroughs() {
		try {
			String sql = """
				SELECT borough, neighbourhood
				FROM Neighbourhoods
				ORDER BY borough
			""";

			PreparedStatement statement = connection.prepareStatement(sql);

			System.out.println("Fetching Data...");
			ResultSet result = statement.executeQuery();

			String headerFormat = "| %-13s | %-29s |";
			String bodyFormat = "| %13s | %-29s |";

			output.clear();
			header = String.format(headerFormat, "Borough", "Neighbourhood");
			divider = "+---------------+-------------------------------+";

			while (result.next()) {
				String borough = result.getString("borough");
				String neighbourhood = result.getString("neighbourhood");

				output.add(String.format(bodyFormat, borough, neighbourhood));
			}

			result.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void listingsForHost(int input) {
		try {
			String sql = """
				SELECT Listings.listing_name, Listings.listing_id
				FROM Listings
				WHERE Listings.host_id IN (
					SELECT Hosts.host_id FROM HOSTS where Hosts.host_id = ?
				)
			""";

			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setInt(1, input);

			System.out.println("Fetching Data...");
			ResultSet result = statement.executeQuery();

			String headerFormat = "| %-32s | %-64s |";
			String bodyFormat = "| %32d | %-64s |";

			output.clear();
			header = String.format(headerFormat, "ID", "Listing Name");
			divider = "+----------------------------------+------------------------------------------------------------------+";

			while (result.next()) {
				int listing_id = result.getInt("listing_id");
				String listing_name = result.getString("listing_name");

				if (listing_name != null && listing_name.length() > 64) {
					listing_name = listing_name.substring(0, 61);
					listing_name += "...";
				}

				output.add(String.format(bodyFormat, listing_id, listing_name));
			}

			result.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void listingDetails(int input) {
		try {
			String sql = """
				SELECT * FROM Listings
				JOIN Neighbourhoods ON Neighbourhoods.neighbourhood = Listings.listing_neighbourhood
				WHERE Listings.listing_id = ?
			""";

			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setInt(1, input);

			System.out.println("Fetching Data...");
			ResultSet result = statement.executeQuery();

			String headerFormat = "";
			String bodyFormat = """
			ID: %d
			Listing Name: %s
			Listing Description: %s
			Neighbourhood: %s
			Neighbourhood Overview: %s
			Borough: %s
			Property Type: %s
			Accommodates: %d
			Bedrooms: %d
			Bathrooms: %d
			Beds: %d
			Price: %.2f
			Rating: %.3f
			Cleanliness Rating: %.3f
			Check-in Rating: %.3f
			Communication Rating: %.3f
			Location Rating: %.3f""";

			output.clear();
			header = "Detailed data for listing";
			divider = "--------------------------------";

			while (result.next()) {
				int listing_id = result.getInt("listing_id");
				String listing_name = result.getString("listing_name");
				String listing_description = result.getString("listing_description");
				String listing_neighbourhood = result.getString("listing_neighbourhood");
				String neighborhood_overview = result.getString("neighborhood_overview");
				String borough = result.getString("borough");
				String property_type = result.getString("property_type");
				int accommodates = result.getInt("accommodates");
				int bedrooms = result.getInt("bedrooms");
				int bathrooms = result.getInt("bathrooms");
				int beds = result.getInt("beds");
				float price = result.getFloat("price");
				float rating = result.getFloat("rating");
				float cleanliness_rating = result.getFloat("cleanliness_rating");
				float checkin_rating = result.getFloat("checkin_rating");
				float communication_rating = result.getFloat("communication_rating");
				float location_rating = result.getFloat("location_rating");
				
				output.add(String.format(bodyFormat,
					listing_id,
					listing_name,
					listing_description,
					listing_neighbourhood,
					neighborhood_overview,
					borough,
					property_type,
					accommodates,
					bedrooms,
					bathrooms,
					beds,
					price,
					rating,
					cleanliness_rating,
					checkin_rating,
					communication_rating,
					location_rating
				));
			}

			result.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public void pricingWithNO2() {
		try {
			String sql = """
				SELECT
					Neighbourhoods.borough,
					Measurements.measurement_name,
					AVG(Listings.price) as avg_price,
					AVG(AirQuality.measurement_value) as avg_no2_level,
					RANK() OVER (ORDER BY AVG(AirQuality.measurement_value) DESC) as no2_rank
				FROM Listings
				JOIN Neighbourhoods on Listings.listing_neighbourhood = Neighbourhoods.neighbourhood
				JOIN AirQuality on Neighbourhoods.borough = AirQuality.borough
				JOIN Measurements ON AirQuality.measurement_id = Measurements.measurement_id
				WHERE AirQuality.measurement_id = 375
				GROUP BY Neighbourhoods.borough, Measurements.measurement_name
				ORDER BY no2_rank
			""";

			PreparedStatement statement = connection.prepareStatement(sql);

			System.out.println("Fetching Data...");
			ResultSet result = statement.executeQuery();

			String headerFormat = "| %-16s | %-32s | %-16s | %-16s | %-8s |";
			String bodyFormat = "| %16s | %-32s | %-16.2f | %-16.8f | %-8d |";

			output.clear();
			header = String.format(headerFormat, "Borough", "Measurement Name", "Avg Price", "Avg NO2 Level", "Rank");
			divider = "+------------------+----------------------------------+------------------+------------------+----------+";

			while (result.next()) {
				String borough = result.getString("borough");
				String measurement_name = result.getString("measurement_name");
				float avg_price = result.getFloat("avg_price");
				float avg_no2_level = result.getFloat("avg_no2_level");
				int no2_rank = result.getInt("no2_rank");

				output.add(String.format(bodyFormat, borough, measurement_name, avg_price, avg_no2_level, no2_rank));
			}

			result.close();
			statement.close();

			printPage(1);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	// Will format later
	public void filterListingSearch(String input) {
		try {
			output.clear();
			String headerFormat = "| %24s | %-32s | %-64s |";
			String bodyFormat = "| %24d | %-32s | %-64s |";
			divider = "+--------------------------+----------------------------------+------------------------------------------------------------------+";
			String res = "";
			header = String.format(headerFormat, "ID", "Listing Name", "Amenities");
			String sql = "SELECT L2.listing_id, L2.listing_name, \r\n" + //
					"STUFF((\r\n" + //
					"    SELECT ', ' + Amenities.amenity_name from Amenities \r\n" + //
					"    JOIN ComesWith ON ComesWith.amenity_id = Amenities.amenity_id\r\n" + //
					"    JOIN Listings  as L1 ON L1.listing_id = ComesWith.listing_id\r\n" + //
					"    WHERE L1.listing_id = L2.listing_id\r\n" + //
					"    FOR XML PATH('')\r\n" + //
					"    ) ,1, 1, '') as amenity_list\r\n" + //
					"FROM Listings as L2\r\n" + //
					"JOIN Neighbourhoods ON Neighbourhoods.neighbourhood = L2.listing_neighbourhood\r\n" + //
					"JOIN ComesWith ON L2.listing_id = ComesWith.listing_id\r\n" + //
					"JOIN Amenities ON ComesWith.amenity_id = Amenities.amenity_id\r\n" + //
					"JOIN AvailableOn ON AvailableOn.listing_id = L2.listing_id\r\n" + //
					"WHERE Neighbourhoods.borough = ?\r\n" + //
					"AND Amenities.amenity_name IN(";

			String[] splitInput = input.split(" "); // Since we are splitting on ' ', this will prevent injections
			if (splitInput.length < 2) {
				System.out.println(
						"Incorrect Number of input arguments\nCommand should be:\nsl [name of borough] [amenities ...]");

				return;
			}
			String loc = splitInput[0];

			if (loc.contains("_")) {
				String[] split = loc.split("_");
				if (split.length != 2) {
					System.out.println("Error with entering Borough.");
				} else {
					loc = split[0] + " " + split[1];
				}
			}

			for (int i = 1; i < splitInput.length; i++) {
				sql += "?, ";
			}

			sql = sql.substring(0, sql.length() - 2);
			sql += ")";

			String slqEnding = "\nGROUP BY L2.listing_id, L2.listing_name";

			sql += slqEnding;

			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, loc);
			for (int i = 1; i < splitInput.length; i++) { // Applies the amenities to the ? in sql
				statement.setString(i + 1, splitInput[i]);
			}
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				String name = result.getString("listing_name");
				String amenities = result.getString("amenity_list");
				if (name != null && name.length() > 32) {
					String sub = name.substring(0, 29);
					name = sub + "...";
				}

				if (amenities != null && amenities.length() > 64) {
					String sub = amenities.substring(0, 61);
					amenities = sub + "...";
				}

				res = String.format(bodyFormat,
						result.getLong("listing_id"),
						name,
						amenities);

				output.add(res);

			}

			printPage(1);

			result.close();
			statement.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void deleteAll() {
		System.out.println("Dleteing all data...");
		try (Statement statement = connection.createStatement()) {
			String sql = """
				DELETE FROM "Reviews";
				DELETE FROM "Reviewers";
				DELETE FROM "ComesWith";
				DELETE FROM "Amenities";
				DELETE FROM "AvailableOn";
				DELETE FROM "Dates";
				DELETE FROM "Listings";
				DELETE FROM "Hosts";
				DELETE FROM "Neighbourhoods";
				DELETE FROM "AirQuality";
				DELETE FROM "Measurements";
				DELETE FROM "ContributingFactors";
				DELETE FROM "Collisions";
				DELETE FROM "Shootings";
			""";
			connection.setAutoCommit(false);
			statement.executeUpdate(sql);
			connection.commit();
			connection.setAutoCommit(true);

			System.out.println("All data deleted.");
		} catch (SQLException e) {
			e.printStackTrace();

			System.out.println("Error executing update, rolling back...");
			try {
				connection.rollback();
			} catch (SQLException e2) {
				System.out.println("Error rolling back.");
			}
		}
	}

	public void repopulateDatabase() {
		String filename = "AirbnbMSSQL.sql";

		System.out.println("Repopulating the database...");
		try (BufferedReader br = new BufferedReader(new FileReader(filename));
			Statement statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			StringBuilder sql = new StringBuilder();
			String line;
			
			int lineNum = 1;

			System.out.println("Reading and Executing in batches...");
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) continue;
				
				sql.append(line).append(" ");
				lineNum++;
				if (lineNum % 1000 == 0) {
					statement.execute(sql.toString());
					connection.commit();
					sql.setLength(0);
					System.out.println("Processed " + lineNum + " / " + "221202");
				}
			}
			if (sql.length() > 0) {
				statement.execute(sql.toString());
				connection.commit();
			}
			connection.setAutoCommit(true);
			System.out.println("Database repopulated successfully!");

		} catch (Exception e) {
			e.printStackTrace();

			System.out.println("Error executing SQL file, rolling back...");
			try {
				connection.rollback();
			} catch (Exception e2) {
				System.out.println("Error rolling back.");
			}
		}
	}
}
