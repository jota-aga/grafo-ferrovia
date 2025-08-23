package grafo_ferroviaria;

import java.io.File;
import java.util.Scanner;

import grafo_ferroviaria.models.Aresta;
import grafo_ferroviaria.models.Grafo;
import grafo_ferroviaria.models.Vertice;

public class Main {
	public static void main(String[] args) {
		int numVertices, numArestas, tempo;
		String nome, origem, destino;
		Grafo grafo = new Grafo();
		Aresta aresta;
		Vertice vertice;
		double distancia, preco;
		
		try {
			//Localiza o arquivo txt
			File file = new File("ferrovia.txt");
			
			//Passa o arquivo txt para o scanner conseguir fazer o input das informações
			Scanner scan = new Scanner(file);
			
			numVertices = scan.nextInt();
			scan.nextLine();
			
			for(int i = 0; i<numVertices; i++) {
				nome = scan.nextLine();
				vertice = new Vertice(nome);
				grafo.adicionarVertice(vertice);
			}
			
			numArestas = scan.nextInt();
			scan.nextLine();
			
			for(int i = 0; i<numArestas; i++) {
				//Lê toda a linha e usa o split para separa as informações num array de String
				String[] linha = scan.nextLine().split(",");
				origem = linha[0];
				destino = linha[1];
				distancia = Double.parseDouble(linha[2]);
				preco = Double.parseDouble(linha[3]);
				tempo = Integer.parseInt(linha[4]);
				aresta = new Aresta(destino, distancia, preco, tempo);
				
				/*Percorre a lista de vertices para encontrar o vertice e adicionar a aresta a sua lista de arestas. Obs: Não sei se fica melhor substituir
				a classe Vertice por um Map<String, List<Aresta>> porque se for um map a gente não precisa percorrer uma lista é só usar o getKey(nome)
				que já pega a List<Aresta> do vertice*/
				for(Vertice v : grafo.getVertices()) {
					if(v.getNome().equals(origem)) {
						v.adicionarLigacao(aresta);
					}
				}
				
			}
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		grafo.exibirGrafo();
	}
}
