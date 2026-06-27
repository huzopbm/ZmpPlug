# EkonomikaPlugin
Ekonomický plugin pro **Paper MC 1.21.4** (Paper 26.2)

## Funkce
- 💰 **Měna:** drobáky:
- `/balance` (aliasy: `/bal`, `/penize`) – zobraz zůstatek
- `/pay <hráč> <částka>` – pošli peníze (5% daň)
- `/zebrat <hráč>` – požádej o drobáky, příjemce klikne kolik dá
- `/sell` – prodej item v ruce za pevnou cenu z configu
- `/sell all` – prodej vše v inventáři co má cenu
- `/ah` – aukční dům (GUI chest menu)
- `/ah list <cena>` – dej item do AH
- `/ah cancel <ID>` – zruš vlastní výpis
- `/ah search <text>` – hledej v AH
- `/ekonomika` (alias `/eko`) – admin příkazy

## Jak zkompilovat

### Požadavky
- Java 21+
- Maven 3.8+

### Kroky
```bash
cd EkonomikaPlugin
mvn clean package
```

Výsledný soubor: `target/EkonomikaPlugin-1.0.0.jar`

Zkopíruj `.jar` do složky `plugins/` na tvém Paper serveru a restartuj.

## Konfigurace
Po prvním spuštění vznikne `plugins/EkonomikaPlugin/config.yml`.

### Přidat cenu pro item do /sell:
```yaml
sell:
  ceny:
    DIAMOND: 500        # Material name velkými písmeny
    GOLD_INGOT: 50
```

### Změnit daň z /pay:
```yaml
dan:
  pay-procent: 5        # 5% = výchozí
```

### Změnit max výpisů v AH na hráče:
```yaml
ah:
  max-polozek-na-hrace: 5
  expirace-hodin: 48
```

## Oprávnění
| Oprávnění | Výchozí | Popis |
|-----------|---------|-------|
| `ekonomika.balance` | všichni | /balance |
| `ekonomika.pay` | všichni | /pay |
| `ekonomika.zebrat` | všichni | /zebrat |
| `ekonomika.sell` | všichni | /sell |
| `ekonomika.ah` | všichni | /ah |
| `ekonomika.admin` | OP | /eko admin příkazy |
