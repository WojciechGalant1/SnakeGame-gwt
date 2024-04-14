const express = require('express');
const bodyParser = require('body-parser');
const mysql = require('mysql');

const app = express();
const PORT = 3000;

// Tworzenie połączenia z bazą danych
const db = mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'snakegame'
});

// Sprawdzenie połączenia z bazą danych
db.connect((err) => {
    if (err) {
        console.error('Błąd połączenia z bazą danych:', err);
        return;
    }
    console.log('Połączono z bazą danych MySQL');
});

// Ustawienie parsera dla danych JSON
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use((req, res, next) => {
    res.setHeader('Access-Control-Allow-Origin', '*'); 
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    next();
});

// Endpoint dla przesyłania danych z formularza do bazy danych
app.post('/submit-form', (req, res) => {
    const { login, score } = req.body;

    // Zapytanie SQL do wstawiania danych
    const sql = 'INSERT INTO scoreboard (name, points) VALUES (?, ?)';
    db.query(sql, [login, score], (err, result) => {
        if (err) {
            console.error('Błąd podczas wstawiania danych:', err);
            res.status(500).json({ error: 'Wewnętrzny błąd serwera' });
            return;
        }
        console.log('Dane wstawione pomyślnie');
        res.status(200).json({ message: 'Dane wstawione pomyślnie' });
    });
});

// Endpoint do wyświetlania całej zawartości tabeli
app.get('/scoreboard', (req, res) => {
    // Zapytanie SQL do pobrania wszystkich rekordów z tabeli
    const sql = 'SELECT * FROM scoreboard';
    db.query(sql, (err, result) => {
        if (err) {
            console.error('Błąd podczas pobierania danych:', err);
            res.status(500).json({ error: 'Wewnętrzny błąd serwera' });
            return;
        }
        console.log('Dane pobrane pomyślnie');
        res.status(200).json(result);
    });
});

// Uruchomienie serwera
app.listen(PORT, () => {
    console.log(`Serwer działa na porcie ${PORT}`);
});
