package com.barolab.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import lombok.extern.java.Log;

@Log
public class ExcelObjectReader extends ExcelObjectDefault {

	private Field[] vfields = new Field[100];
	private DataFormatter dataFormatter = new DataFormatter();
	private BeanClass beanClass = new BeanClass();
	private List<BeanAttribute> colAttrs = new LinkedList<BeanAttribute>();

	public List<?> read(Class clazz, String sheetname, String filename) {
		beanClass.init(clazz);
		try {
			return read0(clazz, sheetname, filename);
		} catch (EncryptedDocumentException | InvalidFormatException | InstantiationException | IllegalAccessException
				| IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public List<?> read0(Class clazz, String sheetname, String filename) throws EncryptedDocumentException,
			InvalidFormatException, IOException, InstantiationException, IllegalAccessException {

		// this.clazz = clazz;
		Sheet sheet = null;
		FileInputStream fileInputStream = new FileInputStream(filename);
		Workbook workbook = WorkbookFactory.create(fileInputStream);
		if (sheetname == null) {
			sheet = workbook.getSheetAt(0);
		} else {
			sheet = workbook.getSheet(sheetname);
		}
		if (sheet == null) {
			log.severe("sheet name = " + sheet);
		}

		/**
		 * make attribute list
		 */
		log.info("make attribute list from row0");
		Row row0 = sheet.getRow(0);
		for (Cell cell : row0) {
			String name = cell.getStringCellValue();
			BeanAttribute attr = beanClass.findAttribute(name);
			if (attr != null) {
				colAttrs.add(attr);
				attr.setXlsColIndex(cell.getColumnIndex());
				log.info("col name = " + name + " attr=" + attr);
			}
		}
		log.info("total attribute size = " + colAttrs.size());

		/**
		 * make value Object list
		 */
		List list = new LinkedList();
		for (Row row : sheet) { // Each Row
			if (row.getRowNum() == 0)
				continue;
			Object valueObject = clazz.newInstance();
			for (BeanAttribute attr : colAttrs) { // Each Activated Attribute
				Cell cell = row.getCell(attr.getXlsColIndex());
				if (cell == null) {
					log.warning("cell is not exist. col=" + attr.getXlsColIndex() + ", row=" + row.getRowNum()
							+ ", sheet=" + sheet.getSheetName());
				} else {
					CellType type = cell.getCellTypeEnum();
					try {
						switch (type.getCode()) {
						case 0: // numeric and date
							if (attr.getType() == int.class) {
								int v0 = (int) cell.getNumericCellValue();
								attr.getSetter().invoke(valueObject, v0);
							}
							if (attr.getType() == Date.class) {
								Date v0 = cell.getDateCellValue();
								attr.getSetter().invoke(valueObject, v0);
							}
							if (attr.getType() == String.class) {
								int v0 = (int) cell.getNumericCellValue();
								String s = "" + v0;
								attr.getSetter().invoke(valueObject, s);
							}
							break;
						case 1: // String
							String value = cell.getStringCellValue();
							attr.getSetter().invoke(valueObject, value);
							break;
						case 3: // BLANK
							break;
						default:
							System.out.println("[ERROR] type = " + type + " name=" + attr.getName());
						}
					} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			list.add(valueObject);
		}
		log.info("read class(" + beanClass.getClazz().getSimpleName() + ") count=" + list.size() + ".");
		return list;
	}
}
