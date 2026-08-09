package br.com.escala24.exception;

public class AdministratorRequiredException
        extends RuntimeException {

    public AdministratorRequiredException() {
        super("Somente um administrador pode analisar a solicitação");
    }
}