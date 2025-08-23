package grafo_ferroviaria.models;

import java.util.ArrayList;
import java.util.List;

public class Grafo {
	List<Vertice> vertices;
	
	public Grafo() {
		vertices = new ArrayList<>();
	}
	
	public void adicionarVertice(Vertice vertice) {
		vertices.add(vertice);
	}
	
	public void exibirVertice(String nome) {
		for(Vertice v : vertices) {
			if(v.getNome().equals(nome)) {
				System.out.println("Origem: "+v.getNome());
				for(Aresta a : v.getLigacoes()) {
					System.out.printf("Destino: %s | Preço: %.1f | Distância: %.1fkm | Tempo: %dmin\n", a.getDestino(), a.getPreco(), a.getDistancia(), a.getTempo());
				}
				return;
			}
		}
		
		System.out.println("Esse ponto não existe!");
	}
	
	public void exibirGrafo() {
		for(Vertice v : vertices) {
				System.out.println("Origem: "+v.getNome());
				for(Aresta a : v.getLigacoes()) {
					System.out.printf("Destino: %s | Preço: %.1f | Distância: %.1fkm | Tempo: %dmin\n", a.getDestino(), a.getPreco(), a.getDistancia(), a.getTempo());
				}
			}
	}
	
	public List<Vertice> getVertices(){
		return vertices;
	}
}
