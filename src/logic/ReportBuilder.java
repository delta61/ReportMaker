package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.docx4j.openpackaging.exceptions.Docx4JException;

import plain.Line;
import plain.Table;

public class ReportBuilder{
	


	private String sourceFileName;

	private Line currentLine;
	private boolean eof = false; //end of file
	private CSVReader csvReader;
	private Table bonusForEtalon = null;
	
	private DOCXworker dw;
	
	private boolean isBonus;
	private boolean isRostov;
	private boolean isCities;
	private boolean isAddWords;

	private int lastBonusPos;
	private int lastCitiesPos;
	
	private boolean isSepBonus;
	private boolean isSepRostov;
	private boolean isSepOther;
	private boolean isSepAddw;
	
	private boolean isNeedTableChecking;
	private boolean leaveEmpty;
	private boolean isBestLineToTop;
	
	public ReportBuilder(String sourceFileName, HashMap<String, String> values) {
		//��, ���������. �� �����!11!!
		this.sourceFileName = sourceFileName;
		this.isBonus = values.containsKey("isBonus");
		this.isRostov = values.containsKey("isRostov");
		this.isCities = values.containsKey("isCities");
		this.isAddWords = values.containsKey("isAddWords");
		this.lastBonusPos = Integer.valueOf(values.get("lastBonusPos"));
		this.lastCitiesPos = Integer.valueOf(values.get("lastCitiesPos"));
		this.isSepBonus = values.containsKey("isSepBonus");
		this.isSepRostov = values.containsKey("isSepRostov");
		this.isSepOther = values.containsKey("isSepOther");
		this.isSepAddw = values.containsKey("isSepAddw");
		this.isNeedTableChecking = values.containsKey("isNeedTableChecking");
		this.leaveEmpty = values.containsKey("leaveEmpty");
		this.isBestLineToTop = values.containsKey("isBestLineToTop");
	}
	
	
	public DOCXworker buildReport(String docExamplePath){

		csvReader = new CSVReader(this.sourceFileName);

		//for DOCX
		dw = null;
		try {
			dw = new DOCXworker(docExamplePath, " ");
		} catch (Docx4JException | JAXBException e) {
			e.printStackTrace();
		}

		doBonus(isBonus);
		doRostov(isRostov);
		doCities(isCities | isAddWords);
		
		csvReader.close();

		
		//��������� DOCX
		dw.removeTemplateTable();

		System.out.println("����������� ������ ������� ���������");
		
		return dw;
	}
	
	
	/**
	 * ������ �����
	 */

	private void doBonus(boolean isNeed){
		if(!isNeed)
			return;
		currentLine = csvReader.readLine();
		checkEOF(); 
	
		bonusForEtalon = new Table();
		
		while(!csvReader.isTableSeparator(currentLine.getString()) & !eof){ //���� ��������� ������ �� �����������
			bonusForEtalon.addLine(currentLine);

			currentLine = csvReader.readLine();
			checkEOF();
			if(eof){
				break;
			}
		}
		
		
		ArrayList<Table> bonuses = new ArrayList<Table>(); 
		bonuses.add(bonusForEtalon);
		
		String bonusstring = "�����: ������� ������� ����� �� ������������ �������� ��� �������� ������ - ���������� �������� �������� ���������� �����������!";
		Table bonus = Table.buildBestTable(bonuses, lastBonusPos, false, this.leaveEmpty, this.isBestLineToTop);//���� ������������ ����� ��� ���� �������, ������� ���� ������ ��������� �� ���� ������� :D
		//��� �������� � .docx
		if(isSepBonus){
			dw.insertNewTableWithTwoColumns(bonus, bonusstring, this.lastBonusPos);
		}else{
			dw.insertNewTable(bonus, bonusstring, this.lastBonusPos);
		}
		
		
		if(isSepBonus){
			bonus = Table.buildBestTable(bonuses, lastBonusPos, true, this.leaveEmpty, this.isBestLineToTop);
			//��� �������� � .docx
			dw.insertNewTableWithTwoColumns(bonus, "GOOGLE", this.lastBonusPos);
		}
	}
	
	/**
	 * ������ ������
	 */

	private void doRostov(boolean isNeed){
		if(!isNeed)
			return;
		
		if(eof)
			System.out.println("������ ������ � ������, �� ���� �������� ����������((");
		
		ArrayList<Table> rostovTables = new ArrayList<Table>();
		
		for(int i = 0; i < 4; i++){ //4 �������
			Table table = new Table();
			
			currentLine = csvReader.readLine();
			checkEOF();
			while(!csvReader.isTableSeparator(currentLine.getString())){ //���� ��������� ������ �� �����������
				table.addLine(currentLine);
				currentLine = csvReader.readLine();
				checkEOF();
				if(eof){
					if(i == 3){
						break;//���� ���� �������������
					}
					else{
						System.out.println("�� ������� ������ �� �������, ��� ��� �� ���������");
					}
				}
			}
			
			rostovTables.add(table);
		}
		
		//��������� ����������� �������!
		if(isNeedTableChecking){
			if(this.bonusForEtalon != null){
				TableFixer.checkTables(rostovTables, this.bonusForEtalon);
			}else{
				System.out.println("����� �������� ����������� ��������, �� �� ���� ������");
			}
		}
		
		
		Table rostovTABLE = Table.buildBestTable(rostovTables, lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);
		//��� �������� � .docx
		if(isSepRostov){
			dw.insertNewTableWithTwoColumns(rostovTABLE, "� ����������� ������: ������-��-����", lastCitiesPos);
		}else{
			dw.insertNewTable(rostovTABLE, "� ����������� ������: ������-��-����", lastCitiesPos);
		}
		
		if(isSepRostov){
			rostovTABLE = Table.buildBestTable(rostovTables, lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
			//��� �������� � .docx
			dw.insertNewTableWithTwoColumns(rostovTABLE, "GOOGLE", lastCitiesPos);
		}
	}
	
	
	/**
	 * //������ ������ + ���. �����

	 * ������� ������� ��� ��� �������, � ����� ����� �� ��� � ���������� �� ���.�����. 
	 * ��������, � ������ ������� ��� ����� "xyz...", � ������ "� xyz...e" ��� e - ���� ������, ���� ����� �� ���������� � ���������� ������
	 * �� ����� � ������ ������ ��� �������(������ ���� ��� ��������� ��������� �� 3 �������?))
	 * � ��� ������ ��������� ��������� "[�|��] xyz...]
	 * ���� ������� - ��� ��� ��� ���� ������� � ��������, ��� - �������� ���. �����, ��������� ��������� ����� doAddWords
	 */
	
	private void doCities(boolean isNeed){
		if(!isNeed)
			return;
		
		if(eof) 
			System.out.println("������ ������ ������ � ������, �� ���� �������� ����������((");
		
		ArrayList<Table> oneCityTables = new ArrayList<Table>();
		while(!eof){
			Table table = new Table();
			
			
			currentLine = csvReader.readLine();
			checkEOF();
			
			if(eof)
				System.out.println("��������� ����� ����� ��� ��������� ������ �������");
			
			while(!csvReader.isTableSeparator(currentLine.getString())){ //���� ��������� ������ �� �����������
				table.addLine(currentLine);
				currentLine = csvReader.readLine();
				checkEOF();
				if(eof){
					break;
				}
			}
			
			oneCityTables.add(table);
		}
		
		
		//������� ��� ������� � ����� �����, �� �����
		if(isNeedTableChecking){
			if(this.bonusForEtalon != null){
				TableFixer.checkTables(oneCityTables, this.bonusForEtalon);
			}else{
				System.out.println("����� �������� ����������� ��������, �� �� ���� ������");
			}
		}
		
		if(!isAddWords & oneCityTables.size() % 2 == 1){
			System.out.println("� ���������� ������� ���������� ���. ����, � ������ �� ���. ������� - �������� ���-��.\n���-�� ��� �� ���, �������");
		}
		
		boolean addWordsAreFound = false;
		
		ArrayList<Table> realCities = new ArrayList<>();
		ArrayList<Table> realCitiesGoo = new ArrayList<>();
		for(int i = 0; i < oneCityTables.size(); i+=2){
			if(i+1 < oneCityTables.size()){
				Table city1 = oneCityTables.get(i);
				String city1add = TableFixer.getAdditionalPhrase(city1);
				Table city2 = oneCityTables.get(i+1);
				String city2add = TableFixer.getAdditionalPhrase(city2);
				System.out.println(city1add + ";" + city2add);
				//�� ���� ����� ������
				//��� ��� ��, ��� ��������� � �������� ��� ������� doCities
				//����� ���� ���������� ����� ����������, �� [�|��] �������� ��������������� ���������, ������� ����� ������ �����
				Pattern p1;
				Matcher m1;
				Pattern p2;
				Matcher m2;
				if(city1add.length() < city2add.length()){
					p1 = Pattern.compile("^� " + city1add.substring(0, 2) + ".*$");
					m1 = p1.matcher(city2add.substring(0, 4));
					p2 = Pattern.compile("^�� " + city1add.substring(0, 2) + ".*$");
					m2 = p2.matcher(city2add.substring(0, 5));
				}else{
					p1 = Pattern.compile("^� " + city2add.substring(0, 2) + ".*$");
					m1 = p1.matcher(city1add.substring(0, 4));
					p2 = Pattern.compile("^�� " + city2add.substring(0, 2) + ".*$");
					m2 = p2.matcher(city1add.substring(0, 5));
				}
				
				
				if(m1.matches() | m2.matches()){ //���� ���� �� ��������� ������, ������, ��������� - ������, � ������ � ������., � ������� � ���������� ������ � ��������� �|��
					ArrayList<Table> oneCityTable = new ArrayList<>();
					oneCityTable.add(city1);
					oneCityTables.remove(city1);
					oneCityTable.add(city2);
					oneCityTables.remove(city2);
					i-=2;
					
					Table cityForReal = Table.buildBestTable(oneCityTable, lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);
					realCities.add(cityForReal);
					if(isSepOther){
						Table cityForRealGoo = Table.buildBestTable(oneCityTable, lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
						realCitiesGoo.add(cityForRealGoo);
					}
					
				}else{
					//��� �������� �� ������� �� ����� ������ � �����, ������, ��� �� ������ � ���. �����(�� ��� �������� ����������, �� ��� � ��� �� ��� ��� ���������������, ������, ������)
					addWordsAreFound = true;
					System.out.println("������� ���. �����!");
					break;
				}
			
			}else{
				//���� �������� ��������� ��������, ������� ����� - � ���.�������
				addWordsAreFound = true;
				break;
			}
		}
		
		if(realCities.isEmpty()){
			System.out.println("�������������� ������� �� ���� �������!");
		}else{
			Table citiesToPrint = new Table(realCities);
			
			//��� �������� � .docx
			if(isSepOther){
				dw.insertNewTableWithTwoColumns(citiesToPrint, "� ����������� ������ �������", lastCitiesPos);
			}else{
				dw.insertNewTable(citiesToPrint, "� ����������� ������ �������", lastCitiesPos);
			}
			
			if(isSepOther){
				citiesToPrint = new Table(realCitiesGoo);
				//��� �������� � .docx
				dw.insertNewTableWithTwoColumns(citiesToPrint, "GOOGLE", lastCitiesPos);
			}
		}
		
		if(addWordsAreFound){
			doAddWords(oneCityTables);
		}
		
	}

	private void doAddWords(ArrayList<Table> tables){
		Table addWordsToPrint = new Table();
		Table addWordsToPrintGoo = new Table();
		for(Table tab : tables){
			ArrayList<Table> temp = new ArrayList<>();
			temp.add(tab);
			Table addWord = Table.buildBestTable(temp, lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);
			addWordsToPrint.plusTable(addWord);
			if(isSepAddw){
				addWord = Table.buildBestTable(temp, lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
				addWordsToPrintGoo.plusTable(addWord);
			}
		}
		//��� �������� � .docx
		if(isSepAddw){
			dw.insertNewTableWithTwoColumns(addWordsToPrint, "� ���. �������", lastCitiesPos);
		}else{
			dw.insertNewTable(addWordsToPrint, "� ���. �������", lastCitiesPos);
		}

		if(isSepAddw){
			//��� �������� � .docx
		dw.insertNewTableWithTwoColumns(addWordsToPrintGoo, "GOOGLE", lastCitiesPos);
		}
	}
	
	
	private void checkEOF(){ // ����� ���������, �� ���������� �� ���� (���� ����������, ����� CSVReader ������ ��������� ������� ���������� null
		if(currentLine == null)
			eof = true;
	}
}
