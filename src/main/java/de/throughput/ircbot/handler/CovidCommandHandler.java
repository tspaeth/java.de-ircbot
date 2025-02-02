package de.throughput.ircbot.handler;

import static de.throughput.ircbot.Util.urlEnc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import com.google.gson.Gson;

import de.throughput.ircbot.api.Command;
import de.throughput.ircbot.api.CommandEvent;
import de.throughput.ircbot.api.CommandHandler;

/**
 * Command handler providing information about the ongoing COVID-19 pandemic.
 */
@Component
public class CovidCommandHandler implements CommandHandler {

    private static final URI API_URL_ALL = URI.create("https://corona.lmao.ninja/v2/all");
    private static final URI API_URL_COUNTRY = URI.create("https://corona.lmao.ninja/v2/countries/");

    private static final Command CMD_COVID = new Command("covid", "Usage: !covid [<country>] - get covid stats");

    @Override
    public boolean onCommand(CommandEvent command) {
        if (command.getCommand()
                .equals(CMD_COVID)) {
            getCovidStats(command);
            return true;
        }
        return false;
    }

    @Override
    public Set<Command> getCommands() {
        return Set.of(CMD_COVID);
    }

    private void getCovidStats(CommandEvent command) {
        if (command.getArgLine()
                .isPresent() && "#java.de".equals(command.getArgLine()
                .get())) {
            Path data = Paths.get("covid-#java.de.json");
            if (Files.exists(data)) {
                try {
                    CovidItem item = new Gson().fromJson(Files.readString(data), CovidItem.class);
                    if (item != null) {
                        command.respond(item.toString());
                    } else {
                        command.respond("that didn't work");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    command.respond("that didn't work");
                }
            } else {
                command.respond("I wouldn't know");
            }
            return;
        }

        URI uri = command.getArgLine()
                .map(line -> URI.create(API_URL_COUNTRY + urlEnc(line)))
                .orElse(API_URL_ALL);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();

        HttpClient.newHttpClient()
                .sendAsync(request, BodyHandlers.ofString())
                .thenAccept(httpResponse -> processResponse(command, httpResponse));
    }

    private void processResponse(CommandEvent command, HttpResponse<String> httpResponse) {
        if (httpResponse.statusCode() == 200) {
            CovidItem item = new Gson().fromJson(httpResponse.body(), CovidItem.class);
            if (item != null) {
                command.respond(item.toString());
            } else {
                command.respond("that didn't work");
            }
        } else {
            command.respond("" + httpResponse.statusCode());
        }
    }

    @Getter
    @Setter
    private static class CovidItem {

        private String country;
        private Long cases;
        private Long recovered;
        private Long deaths;
        private Long updated;
        private Long active;
        private Long affectedCountries;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (country != null) {
                sb.append(country)
                        .append(": ");
            }
            sb.append(String.format("confirmed: %d, active: %d, deaths: %d", cases, active, deaths));
            return sb.toString();
        }
    }

}
