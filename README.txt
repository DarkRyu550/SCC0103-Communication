
#################################################
### SCC0103 - Programação Orientada a Objetos ###
#################################################
### Projeto Final: Comunicação P2P            ###
### Categoria:     Desastres Naturais         ###
###                                           ###
### Autores:                                  ###
###     - Matheus Branco Borella  (11218897)  ###
###     - Natan Bernardi Cerdeira (11218489)  ###
###                                           ###
### Plataforma: Android                       ###
### Língua:     Java 8                        ###
#################################################

##############
# Introdução #
##############
Como uma ideia para a categoria de desastres naturais, propomos um programa que
permitisse a comunicação entre dispositivos móveis fisicamente próximos, sem o
auxílio de nenhum intermediador para a comunicação (leia-se roteadores ou
computadores). O programa entregue consegue proporcionar essa comunicação entre
dois ou mais dispositivos, com certas restrições.

#################
# Implementação #
#################
A implementação da comunicação é feita em duas camadas, em uma camada temos a
definição de um protocolo de comunicação peer-to-peer. Os tipos e classes chave
na implementação do protocolo são:
 - `net.xn__n6x.communication.identity.Id` [1]: Um tipo de dados opaco cuja
   função é de unicamente identificar um dispositivo.
 - `net.xn__n6x.communication.control.Packet`: Um tipo de dados não opaco que
   embala os dados transmitidos juntamente com informação de transporte, mais
   especificamente: remetente, destinatário e rota.
 - `net.xn__n6x.communication.control.Router`: A classe responsável por definir
   o comportamento de propagação e de direcionamento de pacotes e executá-lo.

Os seguintes tipos também são definidos para o protocolo de comunicação, mas
representam aspectos secundários, que não foram realizados em seu potencial
máximo em nossa implementação, mas que poderiam ser extendidos:
 - `net.xn__n6x.identity.Profile`: Um tipo de dados não opaco que carrega
   consigo informações sobre o usuário que estão fora do escopo técnico da
   implementação, por assim dizer. Informações como o nome do usuário, sua
   imagem de perfil, etc.



Na segunda camada, mais baixa, o protocolo peer-to-peer definido na primeira
camada foi implementado usando as capacidades propiciadas pelo sistema Android.
A capacidade que foi escolhida nessa implementação é a Wifi Direct, porque
propiciava a melhor experiência que ainda pudesse ser testada com nossos
recursos limitados em termos de material. Por exemplo, não temos nenhum celular
com suporte a Bluetooth LE 5.0, acesso ao qual teria nos permitido implementar o
protocolo de uma maneira melhor.

O protocolo foi implementado como um serviço, em
`net.xn__n6x.communication.watchdog.Watchdog` e suas classes companheiras, todas
no mesmo pacote.

Para entrar em detalhes, o serviço funciona como uma máquina de
estado finito, com transições síncronas ocorrendo em dois casos: no primero, a
cada evento de conexão bem sucedida com outro dispositivo e, no segundo caso,
quando há um evento de envio de mensagem ou durante a inicialização. A máquina
possui três estados, dois deles sendo estados nos quais o serviço ativamente
busca por conexões e o outro sendo um estado no qual o serviço passivamente
espera por novos eventos externos.

Em suma, o comportamento do serviço começa no processo de descoberta, no qual
o servico conecta-se a todos os dispositivos próximos e busca por informações
relacionadas ao protocolo de transmissão, para montar a sua topologia de rede.
Essa topologia de rede, então, é usada pelo roteador para definir como os
pacotes atualmente nesse dispositivo serão tratados. Depois que esse estágio se
conclui, ele vai se conectar com todos os dispositivos para os quais ele deve
mandar um pacote, e se conecta a cada um deles, mandando os pacotes.

Note que os pacotes não necessitam ser diretamente enviados pelo remetente ao
destinatário, pode-se enviar a qualquer número de terceiros que então pela
estrutura de roteador farão o seu melhor para encaminhar o pacote recebido
ao real destinatário. Isso permite que se estabeleça transmissão de dados entre
dispositivos que estão fora do alcance um do outro por meio de um terceiro,
situado entre os dois, ao qual tanto o primeiro quanto o segundo têm acesso.



Por fim, no pacote `net.xn__n6x.communication.android` ficam as atividades e
código relacionado à interface de usuário no Android. Essa interface permite o
envio e recebimento de mensagens e o início de conversas com dispositivos em
raio de alcance. No pacote raíz `net.xn__n6x.communication` há a classe
`Assertions`, cujo propósito é de proporcionar utilidades para garantir que o
programa falhará de maneira clara e imediata quando alguma invariante de código
é violada ou é encontrado algum estado inválido durante a execução.

--
[1]: O nome do pacote vem do domínio 狼.net, cuja representação punycode é
xn--n6x.net. Como Java por definição não considera aceitável o uso de caracteres
fora do que está na sessão 3.8 da especificação da linguagem Java, excluindo
tanto caracteres internacionais quanto o hífen, a transcrição mais próxima
possível é "xn__n6x.net".


########################
# Testes Automatizados #
########################

O código relacionado à interface gráfica ou a transmissão via Wifi Direct não é
coberto em nenhum caso de teste pois esse código necessariamente depende de
fatores externos ao código de teste, como cliques de usuários ou a presença de
outros dispositivos em raio de alcance. Como não encontramos nenhuma maneira de
tornar consistentes esses fatores externos, não se poderia esperar que qualquer
teste, quando executado duas vezes, fosse ter o mesmo resultado, ainda que o
código executado fosse o mesmo.

O código relacionado às estruturas de controle é testado usando JUnit 5, com
cobertura de código de aproximadamente 95%, contudo, mesmo essas estruturas,
por algumas implementarem a interface `Parcelable` do Android, não puderam ser
completamente testadas.

A relação entre pacotes e a metodologia de testes é:
	- `net.xn__n6x.communication.android`: Não testado por lidar com código de
	  interface gráfica de usuário, que só pode ser testado dentro do Android.
	- `net.xn__n6x.communication.watchdog`: Não testado por lidar com Wifi
	  Direct e reagir ao ambiente do telefone durante os testes, o que faz com
	  que os resultados das operações se tornem imprevisíveis e dependentes do
	  ambiente no qual o programa é testado.
	- `net.xn__n6x.communication.control`: Todas as classes desse pacote são
	  testadas. Nada específico ao Android introduziu problemas na testagem,
	  então esse pacote foi capaz de atingir 100% de cobertura de métodos e
	  97% de cobertura de linhas de código.
	- `net.xn__n6x.communication.identity`: Todas as classes desse pacote são
	  testadas. Com a excessão dos métodos da classe `Id` que interagem com
	  a classe `Parcel` do Android, que podem somente ser testadas dentro
	  do próprio Android. Com isso em mente, esse pacote foi capaz de atingir
	  91% de cobertura de métodos e 92% de cobertura de linhas de código.
Para informações mais detalhadas da cobertura, foi disponibilizado uma pasta com
a informação de cobertura de código junto com o projeto.

##############
# Compilação #
##############
Para poder compilar e rodar o projeto, os seguintes itens serão necessários:
	- Android SDK
	- JDK 8
	- Um celular Android com nível de API maior ou igual a 25.
	- Ferramentas de compilação para o Android 10 (API 29), instaladas a partir
	  do Android SDK Manager.
Para construir o projeto basta rodar:
	$ ./gradlew build

##############
# Instalação #
##############
Para instalar o projeto é necessário rodar:
	$ ./gradlew installDebug
Ou, opcionalmente, também pode se instalar o projeto usando o APK pré-compilado
que é distribuído junto com os arquivos do projeto.

