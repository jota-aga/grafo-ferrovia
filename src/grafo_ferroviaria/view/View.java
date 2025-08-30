package grafo_ferroviaria.view;

import java.util.HashMap;
import java.util.Scanner;

import grafo_ferroviaria.enums.Metrica;
import grafo_ferroviaria.models.GenericGraph;
import grafo_ferroviaria.models.Rail;
import grafo_ferroviaria.models.TrainStation;

public class View {
	private Scanner scanner = new Scanner(System.in);
	private String origem;
	private String destino;
	private Metrica metrica;
	private GenericGraph<TrainStation, Rail> grafo;
	private HashMap<String, TrainStation> stations;

	public View(GenericGraph<TrainStation, Rail> grafo, HashMap<String, TrainStation> stations) {
		this.grafo = grafo;
		this.stations = stations;
	}

	public void principal() {
		System.out.println("Seja bem-vindo ao auxiliar de caminhos ferroviários");
		int opcao = 99;

		while (true) {
			System.out.println("Escolha uma opcao");
			System.out.println("1 - Calcular rota\n0 - Sair\n");
			opcao = scanner.nextInt();
			scanner.nextLine();
			switch (opcao) {
				case 1:
					this.escolherCaminhoOrigem();
					this.escolherMetrica();

					try {
						var g = grafo.shortestPath(stations.get(origem), stations.get(destino), Rail::time, null);
						if (g.cost == Double.POSITIVE_INFINITY) {
							System.out.printf("\nNão existe caminho entre %s e %s", origem, destino);
						} else {
							System.out.println("\n=== Menor caminho (TEMPO) ===");
							System.out.println(String.join(" -> ", g.path.stream().map(TrainStation::name).toList()));
							System.out.printf("Tempo total: %.2f min%n", g.cost);
						}
					} catch (IllegalArgumentException e) {
						System.out.println("\nErro no cálculo de rota: " + e.getMessage());
					}

					break;
				case 0:
					System.out.println("SAINDO...");
					return;
				default:
					System.out.println("Selecione uma Opção válida");
					break;
			}
		}
	}

	public void escolherCaminhoOrigem() {
		System.out.println("Digite o nome da estação que você está atualmente");
		this.origem = scanner.nextLine();

		System.out.println("Digite o nome da estação de destino");
		this.destino = scanner.nextLine();
	}

	public void escolherMetrica() {
		while (true) {
			System.out.println("Qual das opções abaixo você deseja economizar\n1 - Distância\n2 - Tempo\n3 - Preço\n");
			int opcao = scanner.nextInt();
			scanner.nextLine();

			switch (opcao) {
				case 1:
					this.metrica = Metrica.DISTANCIA;
					return;
				case 2:
					this.metrica = Metrica.TEMPO;
					return;
				case 3:
					this.metrica = Metrica.PRECO;
					return;
				default:
					System.out.println("Opção inválida");
			}
		}
	}
}
