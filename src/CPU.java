import java.util.LinkedList;

public class CPU {

    public enum Opcode {
        DADO, ___,		    // se memoria nesta posicao tem um dado, usa DADO, se nao usada ee NULO
        JMP, JMPI, JMPIG, JMPIL, JMPIE, ADDI, SUBI, ANDI, ORI, LDI, LDD, STD, ADD, SUB, MULT, LDX, STX, SWAP, STOP;
    }



    // --------------------- definicoes da CPU ---------------------------------------------------------------
    private enum Interrupts {  // possiveis interrupcoes
        noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intSTOP;
    }




    // característica do processador: contexto da CPU ...
    private int pc; 			// ... composto de program counter,
    private PosMemoria ir; 			// instruction register,
    private int[] reg;       	// registradores da CPU
    private Interrupts irpt; 	// durante instrucao, interrupcao pode ser sinalizada
    private int base;   		// base e limite de acesso na memoria
    private int limite; // por enquanto toda memoria pode ser acessada pelo processo rodando
    private LinkedList<Integer> pagsProg;
    // ATE AQUI: contexto da CPU - tudo que precisa sobre o estado de um processo
    // para executar
    // nas proximas versoes isto pode modificar, e vai permitir salvar e restaurar
    // um processo na CPU

    private GerenciadorMemoria gm;   // CPU acessa MEMORIA, guarda referencia 'm' a ela. memoria nao muda. ee sempre a mesma.

    public CPU(GerenciadorMemoria gm) {     // ref a MEMORIA passada na criacao da CPU
        this.gm = gm; 				// usa o atributo 'm' para acessar a memoria.
        reg = new int[8]; 		// aloca o espaço dos registradores
    }

    public void setContext(int _base, int _limite, int _pc) {  // no futuro esta funcao vai ter que ser
        base = _base;                                          //expandida para setar todo contexto de execucao,
        limite = _limite;									   // agora,  setamos somente os registradores base,
        pc = _pc;                                              // limite e pc (deve ser zero nesta versao)
        irpt = Interrupts.noInterrupt;                         // reset da interrupcao registrada
    }

    private boolean legal(int e) {                             // todo acesso a memoria tem que ser verificado
        if ((e < base) || (e > limite)) {                      //  valida se endereco 'e' na memoria ee posicao legal
            irpt = Interrupts.intEnderecoInvalido;             //  caso contrario ja liga interrupcao
            return false;
        };
        return true;
    }

    private PosMemoria getPosMem(int pos){
        int pag = pos / 16;
        int ofsset = pos % 16;
        if(pag!=0 && ofsset == 0){
            pag--;
        }
        pos = pagsProg.get(pag);
        return gm.getMem((pos * 16) + ofsset);
    }

    private void setPosMem(PosMemoria memo, int pos){
        int pag = pos / 16;
        int ofsset = pos % 16;
        if(pag!=0 && ofsset == 0){
            pag--;
        }
        pos = pagsProg.get(pag);
        gm.setMem(memo , (pos * 16) + ofsset);
    }

    public void run(LinkedList<Integer> prog) { 		// execucao da CPU supoe que o contexto da CPU, vide acima, esta devidamente setado
        pagsProg = prog;
        while (true) { 			// ciclo de instrucoes. acaba cfe instrucao, veja cada caso.
            // FETCH
            if (legal(pc)) { 	// pc valido
                ir = getPosMem(pc); 	// busca posicao da memoria apontada por pc, guarda em ir
                // EXECUTA INSTRUCAO NO ir
                switch (ir.opc) { // DADO,JMP,JMPI,JMPIG,JMPIL,JMPIE,ADDI,SUBI,ANDI,ORI,LDI,LDD,STD,ADD,SUB,MULT,LDX,STX,SWAP,STOP;

                    case LDI: // Rd ← k
                        reg[ir.r1] = ir.p;
                        pc++;
                        break;

                    case STD: // [A] ← Rs
                        if (legal(ir.p)) {
                            PosMemoria aux = getPosMem(ir.p);
                            aux.opc = Opcode.DADO;
                            aux.p = reg[ir.r1];
                            pc++;
                            setPosMem(aux, ir.p);
                        };
                        break;

                    case ADD: // Rd ← Rd + Rs
                        reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
                        pc++;
                        break;

                    case ADDI: // Rd ← Rd + k
                        reg[ir.r1] = reg[ir.r1] + ir.p;
                        pc++;
                        break;

                    case STX: // [Rd] ←Rs
                        if(legal(ir.r1)){
                            PosMemoria aux = getPosMem(ir.r1);
                            aux.opc = Opcode.DADO;
                            aux.p = reg[ir.r2];
                            pc++;
                            setPosMem(aux, ir.r1);
                        }
                        break;

                    case SUB: // Rd ← Rd - Rs
                        reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
                        pc++;
                        break;

                    case JMPIG: // If Rc > 0 Then PC ← Rs // Else PC ← PC +1
                        if(reg[ir.r2] > 0) pc = reg[ir.r1];
                        else pc++;
                        break;

                    case JMP: //PC ← k
                        pc = ir.p;
                        break;

                    case JMPI: //PC ← Rs
                        pc = reg[ir.r2];
                        break;

                    case JMPIE: //if Rc = 0 then PC ← Rs // Else PC ← PC +1
                        if(reg[ir.r2] == 0) pc = reg[ir.r1];
                        else pc++;
                        break;

                    case JMPIL: //if Rc < 0 then PC ← Rs  //Else PC ← PC +1
                        if(reg[ir.r2] < 0) pc = reg[ir.r1];
                        else pc++;
                        break;

                    case ANDI: //Rd ←Rd AND k
                        reg[ir.r1] = reg[ir.r1] & ir.p;
                        pc++;
                        break;

                    case ORI: //Rd ←Rd OR k
                        reg[ir.r1] = reg[ir.r1] | ir.p;
                        pc++;
                        break;

                    case LDD: //Rd ← [A]
                        if (legal(ir.p)) {
                            reg[ir.r1] = getPosMem(ir.p).p;
                            pc++;

                        }
                        break;

                    case MULT: //Rd ← Rd * Rs
                        reg[ir.r1] = reg[ir.r1] * reg[ir.r2];
                        pc++;
                        break;

                    case LDX: //Rd ← [Rs]
                        reg[ir.r1] = getPosMem(ir.r2).p;
                        pc++;
                        break;

                    case SWAP: //Rd7←Rd3, Rd6←Rd2, Rd5←Rd1, Rd4←Rd0
                        reg[7] = reg[3];
                        reg[6] = reg[2];
                        reg[5] = reg[1];
                        reg[4] = reg[0];
                        break;

                    case STOP: //  para a execucao
                        irpt = Interrupts.intSTOP;
                        break;

                    case DADO:
                        break;

                    default:
                        break;
                }
            }
            // verifica int - agora simplesmente para programa em qualquer caso
            if (!(irpt == Interrupts.noInterrupt)) {
                System.out.print("Interrupcao ");
                System.out.println(irpt);
                break; // break sai do loop da cpu
            }
        }
    }
}