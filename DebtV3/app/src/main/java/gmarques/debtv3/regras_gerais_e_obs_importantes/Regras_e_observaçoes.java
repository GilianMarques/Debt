package gmarques.debtv3.regras_gerais_e_obs_importantes;

class Regras_e_observaçoes {
    /*      SALVAR TIMESTAMPS EM UTC
     *
     * Apos escrver a info abaixo decidi modificar o codigo pra salvar as stamps das datas de pagamento, recebimento e afins em
     * UTC modificando os setters ods objetos. Nao removi oque foi escrito por fins de documentação mesmo.
     *
     * Apos definir o idioma e data do emulador ara japones e setar sua localização em tokyo, adicionei uma despesa e sincronizei com o meu
     * telefone e o fuso horario foi convertido,  a data de pgto era 01/06/2020 as 00:00:00 no emulador com fuso-horario japones
     * e no meu telefone com fuso-horario brasileiro (logicamante) a data foi alterada para 31/05/2020 as 12:00:00 o fuso horario
     * do brasil é -3 e o do japão é +9, logo a diferença é de 12 horas, armazenei uma timestamp sem qqer informação de fuso-horario
     * e nao faço ideia de como o jodaTime consegue converter a data entre os fuso-horarios sendo que eu nao salvo  o timestamp em UTC
     * que seria o certo a se fazer mas ta funcionando.
     *
     */

    //

}
