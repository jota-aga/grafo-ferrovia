package grafo_ferroviaria.models;

public class Aresta {
	private String destino;
	private double distancia;
	private double preco;
	private int tempo;
	
	public Aresta(String destino, double distancia, double preco, int tempo) {
		this.destino = destino;
		this.distancia = distancia;
		this.preco = preco;
		this.tempo = tempo;
	}

	public String getDestino() {
		return destino;
	}

	public void setDestino(String destino) {
		this.destino = destino;
	}

	public double getDistancia() {
		return distancia;
	}

	public void setDistancia(double distancia) {
		this.distancia = distancia;
	}

	public double getPreco() {
		return preco;
	}

	public void setPreco(double preco) {
		this.preco = preco;
	}

	public int getTempo() {
		return tempo;
	}

	public void setTempo(int tempo) {
		this.tempo = tempo;
	}
	
	
}
