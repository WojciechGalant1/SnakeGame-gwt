package com.example.myproject.client;

import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.thirdparty.json.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGame implements EntryPoint {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 500;
    private static final int UNIT_SIZE = 10;
    private static final int INITIAL_LENGTH = 4;
    private static final int FRAMES_PER_SECOND = 10;
    private static final String API_URL = "http://localhost:3000/submit-form";
    private static final String SCOREBOARD_API_URL = "http://localhost:3000/scoreboard";

    
    private List<Coordinate> snake;
    private Coordinate food;
    private Direction direction = null;
    private boolean gameOver = false;
    private boolean gameStarted = false;
    private long lastFrameTime = 0;
    private int score = 0;

    private Button startButton;
    private Label scoreLabel;
    private TextBox nameTextBox;
    private Button addButton;
    private FlexTable scoreboardTable;

    @Override
    public void onModuleLoad() {
        VerticalPanel verticalPanel = new VerticalPanel();
        Canvas canvas = Canvas.createIfSupported();
        canvas.setCoordinateSpaceWidth(WIDTH);
        canvas.setCoordinateSpaceHeight(HEIGHT);
        final Context2d context = canvas.getContext2d();
        verticalPanel.add(canvas);

        startButton = new Button("Start Game");
        startButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                startGame();
            }
        });
        verticalPanel.add(startButton);

        scoreLabel = new Label("Score: 0");
        verticalPanel.add(scoreLabel);

        nameTextBox = new TextBox();
        nameTextBox.setText("Enter your name");
        nameTextBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if ("Enter your name".equals(nameTextBox.getText())) {
                    nameTextBox.setText("");
                }
            }
        });
        verticalPanel.add(nameTextBox);

        addButton = new Button("Add");
        addButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addScoreToDatabase();
            }
        });
        verticalPanel.add(addButton);

        scoreboardTable = new FlexTable();
        verticalPanel.add(scoreboardTable);

        RootPanel.get().add(verticalPanel);

        snake = new ArrayList<>();

        canvas.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if (gameStarted && !gameOver) {
                    switch (event.getNativeKeyCode()) {
                        case 38: // UP
                            if (direction != Direction.DOWN) direction = Direction.UP;
                            break;
                        case 40: // DOWN
                            if (direction != Direction.UP) direction = Direction.DOWN;
                            break;
                        case 37: // LEFT
                            if (direction != Direction.RIGHT) direction = Direction.LEFT;
                            break;
                        case 39: // RIGHT
                            if (direction != Direction.LEFT) direction = Direction.RIGHT;
                            break;
                    }
                }
            }
        });

        AnimationScheduler animationScheduler = AnimationScheduler.get();
        animationScheduler.requestAnimationFrame(new AnimationScheduler.AnimationCallback() {
            @Override
            public void execute(double timestamp) {
                if (gameStarted && !gameOver) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastFrameTime > 1000 / FRAMES_PER_SECOND) {
                        move();
                        checkCollision();
                        render(context);
                        lastFrameTime = currentTime;
                    }
                }
                animationScheduler.requestAnimationFrame(this);
            }
        });

        // Pobierz wyniki z bazy danych przy załadowaniu strony
        getScoreboardData();
    }

    private void startGame() {
        gameStarted = true;
        gameOver = false;
        snake.clear();
        direction = null;
        for (int i = 0; i < INITIAL_LENGTH; i++) {
            snake.add(new Coordinate(WIDTH / 2 - i * UNIT_SIZE, HEIGHT / 2));
        }
        generateFood();
        startButton.setEnabled(false); // Wyłącz przycisk "Start Game"
        score = 0;
        updateScoreLabel();
    }

    private void move() {
        if (direction == null) return;
        Coordinate head = snake.get(0);
        Coordinate newHead = new Coordinate(head.x, head.y);
        switch (direction) {
            case UP:
                newHead.y -= UNIT_SIZE;
                break;
            case DOWN:
                newHead.y += UNIT_SIZE;
                break;
            case LEFT:
                newHead.x -= UNIT_SIZE;
                break;
            case RIGHT:
                newHead.x += UNIT_SIZE;
                break;
        }
        snake.add(0, newHead);
        if (!newHead.equals(food)) {
            snake.remove(snake.size() - 1);
        } else {
            generateFood();
            score++;
            updateScoreLabel();
        }
    }

    private void checkCollision() {
        Coordinate head = snake.get(0);
        if (head.x < 0 || head.x >= WIDTH || head.y < 0 || head.y >= HEIGHT) {
            gameOver = true;
        }
        for (int i = 1; i < snake.size(); i++) {
            if (head.equals(snake.get(i))) {
                gameOver = true;
                break;
            }
        }
    }

    private void generateFood() {
        Random random = new Random();
        int x = random.nextInt(WIDTH / UNIT_SIZE) * UNIT_SIZE;
        int y = random.nextInt(HEIGHT / UNIT_SIZE) * UNIT_SIZE;
        food = new Coordinate(x, y);
    }

    private void render(Context2d context) {
        context.clearRect(0, 0, WIDTH, HEIGHT);
        if (!gameStarted) {
            context.setFillStyle("#000000");
            context.fillText("Press the arrow keys to start", WIDTH / 2 - 80, HEIGHT / 2);
            return;
        }
        context.setFillStyle("#00FF00");
        for (Coordinate coord : snake) {
            context.fillRect(coord.x, coord.y, UNIT_SIZE, UNIT_SIZE);
        }
        context.setFillStyle("#FF0000");
        context.fillRect(food.x, food.y, UNIT_SIZE, UNIT_SIZE);
        if (gameOver) {
            context.setFillStyle("#000000");
            context.fillText("Game Over!", WIDTH / 2 - 40, HEIGHT / 2);
            startButton.setEnabled(true); // Włącz przycisk "Start Game" po zakończeniu gry
        }
    }

    private void updateScoreLabel() {
        scoreLabel.setText("Score: " + score);
    }

    private void addScoreToDatabase() {
        String name = nameTextBox.getText();
        if (name.isEmpty()) {
            // Wyswietl komunikat błędu, jeśli pole nazwy jest puste
            return;
        }

        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, API_URL);
        requestBuilder.setHeader("Content-Type", "application/json");

        // Tworzenie obiektu JSON zawierającego dane do wysłania
        StringBuilder requestData = new StringBuilder();
        requestData.append("{");
        requestData.append("\"login\": \"").append(name).append("\",");
        requestData.append("\"score\": ").append(score);
        requestData.append("}");

        try {
            // Wysyłanie żądania HTTP z danymi do API
            requestBuilder.sendRequest(requestData.toString(), new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        // Wyswietl komunikat o pomyślnym zapisaniu danych
                        // Możesz również zaktualizować interfejs użytkownika
                        Window.alert("Score saved successfully!");
                    } else {
                        // Wyswietl komunikat o błędzie
                        Window.alert("Failed to save score: " + response.getStatusText());
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    // Wyswietl komunikat o błędzie
                    Window.alert("Failed to save score: " + exception.getMessage());
                }
            });
        } catch (RequestException e) {
            // Wyswietl komunikat o błędzie
            Window.alert("Failed to send request: " + e.getMessage());
        }
    }

    private void getScoreboardData() {
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, SCOREBOARD_API_URL);

        try {
            requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == 200) {
                        updateScoreboardTable(response.getText());
                    } else {
                        Window.alert("Failed to fetch scoreboard data: " + response.getStatusText());
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Window.alert("Failed to fetch scoreboard data: " + exception.getMessage());
                }
            });
        } catch (RequestException e) {
            Window.alert("Failed to send request: " + e.getMessage());
        }
    }

    private void updateScoreboardTable(String jsonData) {
        // Wyczyść tabelę przed aktualizacją
        scoreboardTable.removeAllRows();

        // Ustaw nagłówki kolumn
        scoreboardTable.setText(0, 0, "Name");
        scoreboardTable.setText(0, 1, "Score");

        // Parsowanie danych JSON
        JSONValue jsonValue = JSONParser.parseStrict(jsonData);
        if (jsonValue.isArray() != null) {
            JSONArray jsonArray = jsonValue.isArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.get(i).isObject();
                if (jsonObject != null) {
                    String name = jsonObject.get("name").isString().stringValue();
                    int points = (int) jsonObject.get("points").isNumber().doubleValue();
                    scoreboardTable.setText(i + 1, 0, name); // +1, aby pominąć wiersz nagłówków
                    scoreboardTable.setText(i + 1, 1, String.valueOf(points));
                }
            }
        }

        // Dodaj dodatkowe style do tabeli
        scoreboardTable.setStyleName("scoreboardTable");
    }

    private static class Coordinate {
        int x, y;

        Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Coordinate)) return false;
            Coordinate other = (Coordinate) obj;
            return this.x == other.x && this.y == other.y;
        }
    }

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}