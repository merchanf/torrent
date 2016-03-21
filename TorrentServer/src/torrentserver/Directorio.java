/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package torrentserver;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @author Fabi
 */
public final class Directorio{
    
    private ConcurrentHashMap<Archivo, Torrent> directorio;
    File archivo;
    private ColaDirectorios colaDirectorios;

    public Directorio() {
        try {
            this.colaDirectorios = new ColaDirectorios();
            archivo = new File("directorio.fabi");
            if(!archivo.exists()) {
                archivo.createNewFile();
                FileOutputStream fos = new FileOutputStream(archivo);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                directorio = null;
                oos.writeObject(directorio);
            }
        } catch (FileNotFoundException fnfex) {
        } catch (IOException ioex){
        }
        this.directorio = load();
    }
    
    private ConcurrentHashMap<Archivo, Torrent> load(){
        try{
            FileInputStream fin = new FileInputStream(archivo);
            ObjectInputStream ois = new ObjectInputStream(fin);
            directorio = (ConcurrentHashMap<Archivo,Torrent>) ois.readObject();
            if( directorio == null )
                directorio = new ConcurrentHashMap<>();
            ois.close();
            System.out.println("Directorio cargado correctamente");
            return directorio;
        }catch(IOException | ClassNotFoundException ex){
            System.out.println("Error al cargar el directorio");
            return null;
        }
    }
    
    public List<Zocalo> clientesDisponibles(String nombre){
        Set<Archivo> archivos = directorio.keySet();
        for(Archivo a: archivos){
            if(a.getNombre().equals(nombre)){
                Torrent t =  directorio.get(a);
                return t.getZocalos();
            }
        }
        return null;
    }
    
    public Archivo obtenerArchivo( String nombre ){
        List<Archivo> archivos = obtenerArchivosDisponibles();
        for( Archivo a: archivos ){
            if( a.getNombre().equals(nombre) )
                return a;
        }
        return null;
    }
    
    public void anadirArchivo ( Archivo archivo ){
        new AnadirAchivo(archivo);
        colaDirectorios.guardar();
    }
    
    public List<Archivo> obtenerArchivosDisponibles(){
        List<Archivo> archivos = new ArrayList<>();
        archivos.addAll(this.directorio.keySet());
        return archivos;
    }
    
    public void imprimirArchivosDisponibles(){
        List<Archivo> archivos = obtenerArchivosDisponibles();
        if (archivos == null){
            System.out.println("Directorio nulo");
        }else if( archivos.isEmpty()){
            System.out.println("Directorio vacío");
        }else{
            for( Archivo a: archivos ){
                System.out.println(a.toString());
            }
        }
    }
    
    public void imprimirArchivosDisponibles(List<Archivo> archivos){
        if (archivos == null){
            System.out.println("Directorio nulo");
        }else if( archivos.isEmpty() ){
            System.out.println("Directorio vacío");
        }else{
            for( Archivo a: archivos ){
                System.out.println(a.toString());
            }
        }
    }
    
    public void anadirZocalo ( String nombre, String hash, String ip, String puerto, String peso ){
        Archivo a = obtenerArchivo(nombre);
        Zocalo z = new Zocalo(ip, puerto);
        if( a!=null )
            anadirZocalo(z, a);
        else{
            double p = Double.parseDouble(peso);
            Archivo temp = new Archivo(nombre, hash, p);
            new AnadirAchivo(temp);
        }
    }
    
    public void anadirZocalo ( Zocalo z, Archivo a ){        
        AnadirZocalo anadirZocalo = new AnadirZocalo(a, z);
    }
    
    public Torrent obtenerTorrent(Archivo archivo){
        return directorio.get(archivo);
    }
    
    private class AnadirAchivo implements Runnable{
        Archivo archivo;

        public AnadirAchivo( Archivo archivo ) {
            this.archivo =  archivo;
        }
        
        @Override
        public void run() {
            Torrent torrent = new Torrent(archivo);
            directorio.put(archivo, torrent);
        }
    }     
    
    private class AnadirZocalo implements Runnable{
        Archivo archivo;
        Zocalo zocalo;

        public AnadirZocalo( Archivo archivo, Zocalo zocalo) {
            this.archivo =  archivo;
            this.zocalo = zocalo;
        }
        
        @Override
        public void run() {
            Set<Archivo> archivosDisponibles = directorio.keySet();
            if(!archivosDisponibles.contains(this.archivo))
                anadirArchivo(archivo);
            Torrent torrent = directorio.get(archivo);
            torrent.anadirZocalo(zocalo);
        }
    }
    
    private class ColaDirectorios implements Runnable{
        private ConcurrentLinkedQueue<ConcurrentHashMap<Archivo, Torrent>> colaDirectorios;

        public ColaDirectorios() {
            this.colaDirectorios = new ConcurrentLinkedQueue<>();
        }
        
        public void guardar(){
            this.colaDirectorios.add(directorio);
        }

        @Override
        public void run() {
            while(true){
                ConcurrentHashMap<Archivo, Torrent> d = colaDirectorios.peek();
                if ( d != null ){
                    try{
                        FileOutputStream fos = new FileOutputStream("directorio.fabi");
                        ObjectOutputStream oos = new ObjectOutputStream(fos); 
                        oos.writeObject(d);
                        oos.close();
                    }catch(Exception ex){
                    }
                }
            }
        }
    }
}
