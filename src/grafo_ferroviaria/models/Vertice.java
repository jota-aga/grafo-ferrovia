package grafo_ferroviaria.models;

import java.util.ArrayList;
import java.util.List;

public class Vertice {
	private String nome;
	private List<Aresta> ligacoes;
	
	public Vertice(String nome) {
		this.nome = nome;
		ligacoes = new ArrayList<>();
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public List<Aresta> getLigacoes() {
		return ligacoes;
	}

	public void adicionarLigacao(Aresta aresta) {
		ligacoes.add(aresta);
	}	
}
