package org.exparser.util.databaseplugin.handlers;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;

import java.util.ArrayList;
import java.util.List;

//A simple visitor to visit block nodes
public class BlockVisitor extends  ASTVisitor{
    List<Block> blocks = new ArrayList<>();

    @Override
    public boolean visit(Block node){

        if(node != null){
            blocks.add(node);
        }

        return super.visit(node);
    }

    public List<Block> getBlocks() {
        return blocks;
    }

}