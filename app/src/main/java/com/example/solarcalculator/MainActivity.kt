package com.example.solarcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.solarcalculator.ui.theme.SolarCalculatorTheme
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.PI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolarCalculatorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EnergyCalculatorApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun EnergyCalculatorApp(modifier: Modifier = Modifier) {
    var dailyPower by remember { mutableStateOf("5") }
    var currentStdDev by remember { mutableStateOf("1") }
    var futureStdDev by remember { mutableStateOf("0.25") }
    var energyCost by remember { mutableStateOf("7") }
    var errorMessage by remember { mutableStateOf("") }

    var W1 by remember { mutableStateOf(0.0) }
    var W2 by remember { mutableStateOf(0.0) }
    var profitBefore by remember { mutableStateOf(0.0) }
    var penaltyBefore by remember { mutableStateOf(0.0) }
    var finalProfitBefore by remember { mutableStateOf(0.0) }

    var W3 by remember { mutableStateOf(0.0) }
    var W4 by remember { mutableStateOf(0.0) }
    var profitAfter by remember { mutableStateOf(0.0) }
    var penaltyAfter by remember { mutableStateOf(0.0) }
    var finalProfitAfter by remember { mutableStateOf(0.0) }

    Column(modifier = modifier.padding(16.dp)) {
        TextField(
            value = dailyPower,
            onValueChange = { dailyPower = it },
            label = { Text("Average Daily Capacity (Pc), MW") },
            modifier = Modifier.fillMaxWidth(),
            isError = dailyPower.isBlank() || dailyPower.toDoubleOrNull() == null
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = currentStdDev,
            onValueChange = { currentStdDev = it },
            label = { Text("Current Std Dev (σ1)") },
            modifier = Modifier.fillMaxWidth(),
            isError = currentStdDev.isBlank() || currentStdDev.toDoubleOrNull() == null
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = futureStdDev,
            onValueChange = { futureStdDev = it },
            label = { Text("Future Std Dev (σ2)") },
            modifier = Modifier.fillMaxWidth(),
            isError = futureStdDev.isBlank() || futureStdDev.toDoubleOrNull() == null
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = energyCost,
            onValueChange = { energyCost = it },
            label = { Text("Energy Cost (V), UAH/kWh") },
            modifier = Modifier.fillMaxWidth(),
            isError = energyCost.isBlank() || energyCost.toDoubleOrNull() == null
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Button(onClick = {
            if (dailyPower.isBlank() || currentStdDev.isBlank() || futureStdDev.isBlank() || energyCost.isBlank()) {
                errorMessage = "All fields must be filled!"
                return@Button
            }

            val Pc = dailyPower.toDoubleOrNull()
            val sigma1 = currentStdDev.toDoubleOrNull()
            val sigma2 = futureStdDev.toDoubleOrNull()
            val V = energyCost.toDoubleOrNull()

            if (Pc == null || sigma1 == null || sigma2 == null || V == null) {
                errorMessage = "Please enter valid numerical values!"
                return@Button
            }

            errorMessage = ""

            val P_lower = Pc - sigma2
            val P_upper = Pc + sigma2

            val deltaW1 = integrateNormalDistribution(Pc, sigma1, P_lower, P_upper)
            W1 = Pc * 24 * deltaW1
            profitBefore = W1 * V
            W2 = Pc * 24 * (1 - deltaW1)
            penaltyBefore = W2 * V
            finalProfitBefore = profitBefore - penaltyBefore

            val deltaW2 = integrateNormalDistribution(Pc, sigma2, P_lower, P_upper)
            W3 = Pc * 24 * deltaW2
            profitAfter = W3 * V
            W4 = Pc * 24 * (1 - deltaW2)
            penaltyAfter = W4 * V
            finalProfitAfter = profitAfter - penaltyAfter
        }) {
            Text("Calculate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Before Improvement:")
        Text("Energy without imbalances: ${"%.2f".format(W1)} MWh")
        Text("Profit: ${"%.2f".format(profitBefore)} UAH")
        Text("Penalty: ${"%.2f".format(penaltyBefore)} UAH")
        Text("Total Profit: ${"%.2f".format(finalProfitBefore)} UAH")

        Spacer(modifier = Modifier.height(8.dp))

        Text("After Improvement:")
        Text("Energy without imbalances: ${"%.2f".format(W3)} MWh")
        Text("Profit: ${"%.2f".format(profitAfter)} UAH")
        Text("Penalty: ${"%.2f".format(penaltyAfter)} UAH")
        Text("Total Profit: ${"%.2f".format(finalProfitAfter)} UAH")
    }
}

fun integrateNormalDistribution(Pc: Double, stdDev: Double, P_lower: Double, P_upper: Double): Double {
    val n = 1000
    val step = (P_upper - P_lower) / n
    var area = 0.0
    for (i in 0 until n) {
        val x1 = P_lower + i * step
        val x2 = P_lower + (i + 1) * step
        val y1 = normalDistribution(x1, Pc, stdDev)
        val y2 = normalDistribution(x2, Pc, stdDev)
        area += 0.5 * (y1 + y2) * step
    }

    return area
}

fun normalDistribution(p: Double, Pc: Double, stdDev: Double): Double {
    return (1 / (stdDev * sqrt(2 * PI))) * exp(-((p - Pc).pow(2)) / (2 * stdDev.pow(2)))
}

@Preview(showBackground = true)
@Composable
fun EnergyCalculatorAppPreview() {
    SolarCalculatorTheme {
        EnergyCalculatorApp()
    }
}
