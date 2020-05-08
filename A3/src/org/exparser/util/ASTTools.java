package org.exparser.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;


public class ASTTools {

	public int getLineNumber(ASTNode node){

		
		CompilationUnit nodeRoot = getCompilationUnit(node);
		int lineNumber = 0;
		if(nodeRoot != null){
			lineNumber = nodeRoot.getLineNumber(node.getStartPosition());
		}
		return lineNumber;
	}
	
	private CompilationUnit getCompilationUnit(ASTNode node){
		ASTNode nodeRoot = node.getRoot();
		ASTNode nodeRoots;
		
		while(!(nodeRoot instanceof CompilationUnit)){
			nodeRoots = nodeRoot.getRoot();
		
			if(nodeRoot == nodeRoots){
				nodeRoot = null;
				break;
			}
		}
		
		return (CompilationUnit) nodeRoot;
	}
	
	public void addMarker(ASTNode node, String message, int lineNumber) {
		IFile file = getFile(node);
		
		try {
			IMarker marker = file.createMarker("org.eclipse.core.resources.problemmarker");
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	private IFile getFile(ASTNode node) {
		IFile file = null;
		ICompilationUnit unit = (ICompilationUnit) getCompilationUnit(node).getJavaElement();
		if ( unit == null ) {
			   // not available, external declaration
			}
			else{
				file = (IFile) unit.getResource();			
			}
		return file;
	}
	
	
}
